// Catalog sync worker (run by GitHub Actions on a schedule).
//
// Reads new audio posts from the Telegram channel, downloads each file via the
// bot, uploads it as a GitHub Release asset (stable public URL), and appends it
// to catalog/catalog.json. The client apps just read that public JSON and play
// the direct URLs — no bot token ever ships in the app.
//
// Env:
//   TELEGRAM_BOT_TOKEN  (repo secret)  — the bot; must be an admin of the channel
//   CHANNEL             (default @himusicapp)
//   GITHUB_TOKEN        (provided by Actions)
//   GITHUB_REPOSITORY   (owner/repo, provided by Actions)

const fs = require('fs')
const path = require('path')

const TOKEN = process.env.TELEGRAM_BOT_TOKEN
const CHANNEL = process.env.CHANNEL || '@himusicapp'
const GH_TOKEN = process.env.GITHUB_TOKEN
const REPO = process.env.GITHUB_REPOSITORY // owner/repo
const RELEASE_TAG = 'catalog-media'

const CATALOG_PATH = path.join('catalog', 'catalog.json')
const STATE_PATH = path.join('catalog', 'state.json')

const tg = (method) => `https://api.telegram.org/bot${TOKEN}/${method}`

function readJson(p, fallback) {
  try {
    return JSON.parse(fs.readFileSync(p, 'utf8'))
  } catch {
    return fallback
  }
}

async function ensureRelease() {
  const base = `https://api.github.com/repos/${REPO}/releases`
  const headers = {
    Authorization: `Bearer ${GH_TOKEN}`,
    Accept: 'application/vnd.github+json'
  }
  // Find existing release by tag
  let res = await fetch(`${base}/tags/${RELEASE_TAG}`, { headers })
  if (res.ok) return await res.json()
  // Create it
  res = await fetch(base, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      tag_name: RELEASE_TAG,
      name: 'Catalog media',
      body: 'Audio files for the shared music catalog (auto-managed).',
      make_latest: 'false'
    })
  })
  if (!res.ok) throw new Error('create release failed: ' + (await res.text()))
  return await res.json()
}

async function uploadAsset(release, name, bytes, contentType) {
  // Delete an existing asset with the same name first (idempotent re-runs)
  const existing = (release.assets || []).find((a) => a.name === name)
  if (existing) {
    await fetch(`https://api.github.com/repos/${REPO}/releases/assets/${existing.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${GH_TOKEN}`, Accept: 'application/vnd.github+json' }
    })
  }
  const uploadUrl = release.upload_url.replace(/\{.*\}$/, '') + `?name=${encodeURIComponent(name)}`
  const res = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${GH_TOKEN}`,
      'Content-Type': contentType,
      'Content-Length': String(bytes.length)
    },
    body: bytes
  })
  if (!res.ok) throw new Error('asset upload failed: ' + (await res.text()))
  const json = await res.json()
  return json.browser_download_url
}

async function tgGetFileBytes(fileId) {
  const info = await (await fetch(tg('getFile') + `?file_id=${fileId}`)).json()
  if (!info.ok) return null // e.g. file > 20MB
  const filePath = info.result.file_path
  const bin = await fetch(`https://api.telegram.org/file/bot${TOKEN}/${filePath}`)
  const buf = Buffer.from(await bin.arrayBuffer())
  return { buf, filePath }
}

async function main() {
  if (!TOKEN) {
    console.error('TELEGRAM_BOT_TOKEN not set — skipping.')
    return
  }
  const catalog = readJson(CATALOG_PATH, { songs: [] })
  const state = readJson(STATE_PATH, { offset: 0 })

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
    // Only accept audio-ish files
    const mime = audio.mime_type || ''
    if (post.document && !mime.startsWith('audio/')) continue

    try {
      const got = await tgGetFileBytes(audio.file_id)
      if (!got) {
        console.log('Skipping file >20MB:', audio.file_name || audio.file_id)
        continue
      }
      if (!release) release = await ensureRelease()

      const safeBase = String(u.update_id)
      const ext = (got.filePath.split('.').pop() || 'mp3').toLowerCase()
      const audioUrl = await uploadAsset(
        release,
        `${safeBase}.${ext}`,
        got.buf,
        mime || 'audio/mpeg'
      )

      // Optional cover from the audio thumbnail
      let coverUrl = ''
      const thumb = audio.thumbnail || audio.thumb
      if (thumb && thumb.file_id) {
        const t = await tgGetFileBytes(thumb.file_id)
        if (t) {
          coverUrl = await uploadAsset(release, `${safeBase}.jpg`, t.buf, 'image/jpeg')
          // refresh release assets list so subsequent dedupe sees new asset
          release = await ensureRelease()
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
  fs.writeFileSync(CATALOG_PATH, JSON.stringify(catalog, null, 2) + '\n')
  fs.writeFileSync(STATE_PATH, JSON.stringify(state, null, 2) + '\n')
  console.log(`Done. Added ${added} song(s). New offset: ${state.offset}`)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
