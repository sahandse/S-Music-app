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
// How long to long-poll getUpdates (helps when running right after posting)
const POLL_TIMEOUT = parseInt(process.env.POLL_TIMEOUT || '25', 10)
// Set RESET_OFFSET=1 to re-read from the start of the pending queue
const RESET_OFFSET = process.env.RESET_OFFSET === '1'
// Telegram bot download cap (getFile) — files above this can't be fetched
const MAX_TG_FILE = 20 * 1024 * 1024

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

async function tgJson(method, params = '') {
  return (await fetch(tg(method) + params)).json()
}

async function tgGetFileBytes(fileId) {
  const info = await tgJson('getFile', `?file_id=${fileId}`)
  if (!info.ok) {
    console.log('  getFile failed (usually a >20MB file):', info.description || '')
    return null
  }
  const filePath = info.result.file_path
  const bin = await fetch(`https://api.telegram.org/file/bot${TG_TOKEN}/${filePath}`)
  return { buf: Buffer.from(await bin.arrayBuffer()), filePath }
}

// Diagnose the bot/channel so failures are obvious in the logs.
async function diagnose() {
  const me = await tgJson('getMe')
  console.log('Bot:', me.ok ? '@' + me.result.username : JSON.stringify(me))

  const wh = await tgJson('getWebhookInfo')
  if (wh.ok && wh.result.url) {
    console.log('A webhook is set (' + wh.result.url + ') — deleting it so getUpdates can work.')
  }
  // Always clear any webhook; a live webhook makes getUpdates return 409.
  await tgJson('deleteWebhook', '?drop_pending_updates=false')

  const chat = await tgJson('getChat', `?chat_id=${encodeURIComponent(CHANNEL)}`)
  if (!chat.ok) {
    console.log('⚠ getChat(' + CHANNEL + ') failed — the bot is probably not in the channel:', chat.description || '')
    return
  }
  console.log('Channel:', chat.result.title || CHANNEL)
  const admins = await tgJson('getChatAdministrators', `?chat_id=${encodeURIComponent(CHANNEL)}`)
  const isAdmin = admins.ok && me.ok && admins.result.some((a) => a.user && a.user.id === me.result.id)
  console.log('Bot is a channel admin:', isAdmin)
  if (!isAdmin) {
    console.log('⚠ The bot MUST be an admin of the channel to receive channel posts. Posts made before it became admin are never delivered.')
  }
}

// Build {title, artist} from the audio tags or the post caption ("Artist - Title").
function songMeta(audio, post) {
  let title = audio.title || ''
  let artist = audio.performer || ''
  const cap = (post.caption || '').trim()
  if ((!title || !artist) && cap) {
    const m = cap.match(/^(.+?)\s*[-–—]\s*(.+)$/)
    if (m) {
      artist = artist || m[1].trim()
      title = title || m[2].trim()
    } else {
      title = title || cap
    }
  }
  title = title || audio.file_name || 'آهنگ'
  return { title, artist }
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

  await diagnose()

  const offset = RESET_OFFSET ? 0 : state.offset
  if (RESET_OFFSET) console.log('RESET_OFFSET=1 — re-reading the pending queue from the start.')

  const updRes = await tgJson(
    'getUpdates',
    `?offset=${offset}&timeout=${POLL_TIMEOUT}&allowed_updates=${encodeURIComponent('["channel_post","message"]')}`
  )
  if (!updRes.ok) {
    console.error('getUpdates failed:', JSON.stringify(updRes))
    return
  }
  const updates = updRes.result || []
  if (updates.length === 0) {
    console.log(
      `No new updates (offset=${offset}). Checklist: (1) the bot must be an ADMIN of ${CHANNEL}; ` +
        '(2) only posts made AFTER it became admin are delivered; ' +
        '(3) re-run with RESET_OFFSET=1 if you think an update was skipped.'
    )
    return
  }
  console.log(`Got ${updates.length} update(s).`)

  let release = null
  let maxUpdateId = offset - 1
  let added = 0

  for (const u of updates) {
    maxUpdateId = Math.max(maxUpdateId, u.update_id)
    const post = u.channel_post || u.message
    if (!post) {
      console.log(`- update ${u.update_id}: no post body — skipping`)
      continue
    }
    const audio = post.audio || post.voice || post.document
    if (!audio) {
      console.log(`- update ${u.update_id}: not an audio message (text/photo/...) — skipping`)
      continue
    }
    const mime = audio.mime_type || ''
    if (post.document && !mime.startsWith('audio/')) {
      console.log(`- update ${u.update_id}: document is not audio (${mime}) — skipping`)
      continue
    }
    if (audio.file_size && audio.file_size > MAX_TG_FILE) {
      console.log(
        `- update ${u.update_id}: file is ${(audio.file_size / 1048576).toFixed(1)}MB — over Telegram's 20MB bot download limit, skipping`
      )
      continue
    }

    try {
      const got = await tgGetFileBytes(audio.file_id)
      if (!got) continue
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

      const { title, artist } = songMeta(audio, post)
      catalog.songs.unshift({
        title,
        artist,
        coverUrl,
        audioUrl,
        durationSec: audio.duration || 0
      })
      added++
      console.log(`+ Added: ${artist ? artist + ' — ' : ''}${title}`)
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
