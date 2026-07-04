// Catalog sync worker (run by GitHub Actions on a schedule from the PRIVATE app
// repo, but it writes to a SEPARATE PUBLIC catalog repo so end users' apps can
// read the catalog and stream the audio without any credentials).
//
// It reads new audio posts from the Telegram channel, uploads each file as a
// GitHub Release asset on the PUBLIC repo (stable public URL), and commits an
// updated catalog.json to that public repo. The app reads that public
// catalog.json and plays the direct URLs — no bot token in any client.
//
// Env:
//   TELEGRAM_BOT_TOKEN  (secret) — the bot; must be an admin of the channel
//   CATALOG_PAT         (secret) — a PAT with write access to the public repo
//   CATALOG_REPO        (e.g. sahandse/himusic-catalog)
//   CHANNEL             (default @himusicapp)

const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const TG_TOKEN = process.env.TELEGRAM_BOT_TOKEN
const PAT = process.env.CATALOG_PAT
const CATALOG_REPO = process.env.CATALOG_REPO // owner/repo (public)
const CHANNEL = process.env.CHANNEL || '@himusicapp'
const RELEASE_TAG = 'catalog-media'

const WORKDIR = path.join(process.cwd(), 'catalog-repo')
const tg = (method) => `https://api.telegram.org/bot${TG_TOKEN}/${method}`

function sh(cmd, opts = {}) {
  return execSync(cmd, { stdio: 'pipe', ...opts }).toString().trim()
}

function readJson(p, fallback) {
  try {
    return JSON.parse(fs.readFileSync(p, 'utf8'))
  } catch {
    return fallback
  }
}

async function gh(url, opts = {}) {
  return fetch(url, {
    ...opts,
    headers: {
      Authorization: `Bearer ${PAT}`,
      Accept: 'application/vnd.github+json',
      ...(opts.headers || {})
    }
  })
}

async function ensureRelease() {
  const base = `https://api.github.com/repos/${CATALOG_REPO}/releases`
  let res = await gh(`${base}/tags/${RELEASE_TAG}`)
  if (res.ok) return await res.json()
  res = await gh(base, {
    method: 'POST',
    body: JSON.stringify({
      tag_name: RELEASE_TAG,
      name: 'Catalog media',
      body: 'Audio files for the shared music catalog (auto-managed).'
    })
  })
  if (!res.ok) throw new Error('create release failed: ' + (await res.text()))
  return await res.json()
}

async function uploadAsset(release, name, bytes, contentType) {
  const existing = (release.assets || []).find((a) => a.name === name)
  if (existing) {
    await gh(`https://api.github.com/repos/${CATALOG_REPO}/releases/assets/${existing.id}`, {
      method: 'DELETE'
    })
  }
  const uploadUrl = release.upload_url.replace(/\{.*\}$/, '') + `?name=${encodeURIComponent(name)}`
  const res = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${PAT}`,
      'Content-Type': contentType,
      'Content-Length': String(bytes.length)
    },
    body: bytes
  })
  if (!res.ok) throw new Error('asset upload failed: ' + (await res.text()))
  return (await res.json()).browser_download_url
}

async function tgGetFileBytes(fileId) {
  const info = await (await fetch(tg('getFile') + `?file_id=${fileId}`)).json()
  if (!info.ok) return null // e.g. file > 20MB
  const filePath = info.result.file_path
  const bin = await fetch(`https://api.telegram.org/file/bot${TG_TOKEN}/${filePath}`)
  return { buf: Buffer.from(await bin.arrayBuffer()), filePath }
}

async function main() {
  for (const [k, v] of Object.entries({ TELEGRAM_BOT_TOKEN: TG_TOKEN, CATALOG_PAT: PAT, CATALOG_REPO })) {
    if (!v) {
      console.error(`${k} not set — skipping.`)
      return
    }
  }

  // Clone the public catalog repo
  const cloneUrl = `https://x-access-token:${PAT}@github.com/${CATALOG_REPO}.git`
  fs.rmSync(WORKDIR, { recursive: true, force: true })
  sh(`git clone --depth 1 ${cloneUrl} ${WORKDIR}`)
  sh(`git -C ${WORKDIR} config user.name "catalog-bot"`)
  sh(`git -C ${WORKDIR} config user.email "catalog-bot@users.noreply.github.com"`)

  const catalogPath = path.join(WORKDIR, 'catalog.json')
  const statePath = path.join(WORKDIR, 'state.json')
  const catalog = readJson(catalogPath, { songs: [] })
  const state = readJson(statePath, { offset: 0 })

  const updRes = await (
    await fetch(
      tg('getUpdates') +
        `?offset=${state.offset}&timeout=0&allowed_updates=${encodeURIComponent('["channel_post","message"]')}`
    )
  ).json()
  if (!updRes.ok) {
    console.error('getUpdates failed:', JSON.stringify(updRes))
    return
  }
  const updates = updRes.result || []
  if (updates.length === 0) {
    console.log('No new updates.')
    return
  }

  let release = null
  let maxUpdateId = state.offset - 1
  let added = 0

  for (const u of updates) {
    maxUpdateId = Math.max(maxUpdateId, u.update_id)
    const post = u.channel_post || u.message
    const audio = post && (post.audio || post.voice || post.document)
    if (!audio) continue
    const mime = audio.mime_type || ''
    if (post.document && !mime.startsWith('audio/')) continue

    try {
      const got = await tgGetFileBytes(audio.file_id)
      if (!got) {
        console.log('Skipping file >20MB:', audio.file_name || audio.file_id)
        continue
      }
      if (!release) release = await ensureRelease()

      const ext = (got.filePath.split('.').pop() || 'mp3').toLowerCase()
      const audioUrl = await uploadAsset(release, `${u.update_id}.${ext}`, got.buf, mime || 'audio/mpeg')

      let coverUrl = ''
      const thumb = audio.thumbnail || audio.thumb
      if (thumb && thumb.file_id) {
        const t = await tgGetFileBytes(thumb.file_id)
        if (t) {
          coverUrl = await uploadAsset(release, `${u.update_id}.jpg`, t.buf, 'image/jpeg')
          release = await ensureRelease() // refresh asset list for dedupe
        }
      }

      catalog.songs.unshift({
        title: audio.title || post.caption || audio.file_name || 'آهنگ',
        artist: audio.performer || '',
        coverUrl,
        audioUrl,
        durationSec: audio.duration || 0
      })
      added++
      console.log('Added:', audio.title || audio.file_name)
    } catch (e) {
      console.error('Failed on update', u.update_id, e.message)
    }
  }

  state.offset = maxUpdateId + 1
  fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2) + '\n')
  fs.writeFileSync(statePath, JSON.stringify(state, null, 2) + '\n')

  const changed = sh(`git -C ${WORKDIR} status --porcelain`)
  if (changed) {
    sh(`git -C ${WORKDIR} add catalog.json state.json`)
    sh(`git -C ${WORKDIR} commit -m "sync: ${added} new song(s) from Telegram"`)
    sh(`git -C ${WORKDIR} push origin HEAD`)
    console.log(`Pushed. Added ${added} song(s). New offset: ${state.offset}`)
  } else {
    console.log('No catalog changes.')
  }
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
