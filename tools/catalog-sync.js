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

// Build {title, artist} from the audio tags or the post caption.
// Supported caption formats (checked in this order):
//   1) Labeled lines (recommended, unambiguous):
//        آهنگ: نام آهنگ            (or: title / عنوان / ترانه / song / track)
//        خواننده: نام خواننده       (or: artist / هنرمند / singer)
//   2) Single line "Artist - Title"  (e.g. "شادمهر - دیوونه")
//   3) Two lines: first = song, second = artist
//   4) Anything else -> used as the title
// Strip the promo junk that music-download sites embed in tags/filenames,
// e.g. "[ MyBia2Music.Com ]", "( 320 )", "| Site.ir", stray URLs.
function clean(s) {
  return (s || '')
    .replace(/\.(mp3|m4a|flac|wav|aac|ogg|opus)$/i, '')
    .replace(/\[[^\]]*\]/g, ' ')
    .replace(/\((?:\s*\d{2,3}\s*(?:kbps)?\s*)\)/gi, ' ')
    .replace(/https?:\/\/\S+/gi, ' ')
    .replace(/\b[\w-]+\.(?:com|ir|net|org|info|music|co)\b/gi, ' ')
    .replace(/\s*[|•]\s*/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .replace(/^[\s\-–—_]+|[\s\-–—_]+$/g, '')
    .trim()
}

function songMeta(audio, post) {
  // 1) Caption first — if you caption the post, it wins over messy file tags.
  let title = ''
  let artist = ''
  const cap = (post.caption || '').trim()
  if (cap) {
    const lines = cap
      .split('\n')
      .map((l) => l.trim())
      .filter(Boolean)

    for (const line of lines) {
      const a = line.match(/^(خواننده|هنرمند|صدا|artist|singer)\s*[:：]\s*(.+)$/i)
      const s = line.match(/^(آهنگ|ترانه|عنوان|نام|title|song|track|name)\s*[:：]\s*(.+)$/i)
      if (a) artist = a[2].trim()
      else if (s) title = s[2].trim()
    }
    if (!title && !artist) {
      // Ignore hashtag/label lines when guessing artist & title
      const content = lines
        .map((l) => l.replace(/#[\p{L}\-]+/gu, '').trim())
        .filter(Boolean)
      const dash = (content[0] || '').match(/^(.+?)\s*[-–—]\s*(.+)$/)
      if (content.length >= 2) {
        title = content[0]
        artist = content[1]
      } else if (dash) {
        artist = dash[1].trim()
        title = dash[2].trim()
      } else if (content.length === 1) {
        title = content[0]
      }
    }
  }

  // 2) Fall back to the file's own tags, cleaned of promo junk.
  title = clean(title) || clean(audio.title) || clean(audio.file_name) || 'آهنگ'
  artist = clean(artist) || clean(audio.performer) || ''
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

  if (!Array.isArray(catalog.songs)) catalog.songs = []
  if (!Array.isArray(catalog.videos)) catalog.videos = []

  // Heal past damage: RESET_OFFSET re-runs used to re-add already-synced
  // updates, leaving duplicate entries that crash the apps' list keys.
  const dedupe = (arr, keyOf) => {
    const seen = new Set()
    return arr.filter((x) => {
      const k = keyOf(x)
      if (seen.has(k)) return false
      seen.add(k)
      return true
    })
  }
  const beforeCount = catalog.songs.length + catalog.videos.length
  catalog.songs = dedupe(catalog.songs, (s) => s.audioUrl)
  catalog.videos = dedupe(catalog.videos, (v) => v.videoUrl)
  const removedDupes = beforeCount - (catalog.songs.length + catalog.videos.length)
  if (removedDupes > 0) console.log(`Removing ${removedDupes} duplicate entr(y/ies) from catalog.json`)

  // Manually curated music-video links: the admin edits manual-videos.json in
  // the public repo (GitHub web editor). Each run rebuilds the manual portion
  // of catalog.videos from that file, so add/edit/delete all propagate.
  const manualPath = path.join(WORKDIR, 'manual-videos.json')
  if (!fs.existsSync(manualPath)) {
    fs.writeFileSync(
      manualPath,
      JSON.stringify(
        {
          _help:
            'این فایل را در گیت‌هاب ویرایش کن. برای هر موزیک ویدیو یک آیتم داخل videos اضافه کن: ' +
            'title (نام آهنگ)، artist (خواننده)، coverUrl (لینک عکس کاور، اختیاری)، videoUrl (لینک مستقیم mp4). ' +
            'بعد از ذخیره، با اجرای همگام‌سازی (یا اجرای زمان‌بندی‌شده) در اپ نمایش داده می‌شود.',
          videos: []
        },
        null,
        2
      ) + '\n'
    )
    console.log('Seeded manual-videos.json template in the catalog repo.')
  }
  const manual = readJson(manualPath, { videos: [] })
  const telegramVideos = catalog.videos.filter((v) => v.src !== 'manual')
  const manualVideos = (Array.isArray(manual.videos) ? manual.videos : [])
    .filter((v) => v && v.videoUrl)
    .map((v) => ({
      title: v.title || 'موزیک ویدیو',
      artist: v.artist || '',
      coverUrl: v.coverUrl || '',
      videoUrl: v.videoUrl,
      durationSec: v.durationSec || 0,
      src: 'manual'
    }))
  catalog.videos = dedupe([...manualVideos, ...telegramVideos], (v) => v.videoUrl)
  if (manualVideos.length) console.log(`Merged ${manualVideos.length} manual video link(s).`)

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
  } else {
    console.log(`Got ${updates.length} update(s).`)
  }

  let release = null
  let maxUpdateId = offset - 1
  let added = 0
  // A cover photo posted just BEFORE a song/video is attached to it.
  let pendingCover = null

  for (const u of updates) {
    maxUpdateId = Math.max(maxUpdateId, u.update_id)
    const post = u.channel_post || u.message
    if (!post) {
      console.log(`- update ${u.update_id}: no post body — skipping`)
      continue
    }
    const media = post.audio || post.voice || post.video || post.document
    if (!media) {
      // A standalone photo becomes the cover for the next song/video.
      if (Array.isArray(post.photo) && post.photo.length) {
        pendingCover = post.photo[post.photo.length - 1].file_id
        console.log(`- update ${u.update_id}: cover photo received — will attach to the next post`)
      } else {
        console.log(`- update ${u.update_id}: not audio/video (text/...) — skipping`)
      }
      continue
    }
    const mime = media.mime_type || ''
    const isVideo = !!post.video || (post.document && mime.startsWith('video/'))
    if (post.document && !mime.startsWith('audio/') && !mime.startsWith('video/')) {
      console.log(`- update ${u.update_id}: document is not audio/video (${mime}) — skipping`)
      continue
    }
    if (media.file_size && media.file_size > MAX_TG_FILE) {
      console.log(
        `- update ${u.update_id}: file is ${(media.file_size / 1048576).toFixed(1)}MB — over Telegram's 20MB bot download limit, skipping`
      )
      continue
    }

    try {
      const got = await tgGetFileBytes(media.file_id)
      if (!got) continue
      if (!release) release = await ensureRelease()

      const defaultExt = isVideo ? 'mp4' : 'mp3'
      const ext = (got.filePath.split('.').pop() || defaultExt).toLowerCase()
      const mediaUrl = await uploadAsset(
        release,
        `${u.update_id}.${ext}`,
        got.buf,
        mime || (isVideo ? 'video/mp4' : 'audio/mpeg')
      )

      let coverUrl = ''
      // Prefer a cover photo posted right before; else the media's own thumbnail
      const coverFileId = pendingCover || (media.thumbnail || media.thumb || {}).file_id
      pendingCover = null // consume it whether or not it downloads
      if (coverFileId) {
        const t = await tgGetFileBytes(coverFileId)
        if (t) {
          coverUrl = await uploadAsset(release, `${u.update_id}.jpg`, t.buf, 'image/jpeg')
          release = await ensureRelease() // refresh asset list for dedupe
        }
      }

      // Same update re-read (e.g. RESET_OFFSET) produces the same URL — skip it
      if (
        catalog.songs.some((x) => x.audioUrl === mediaUrl) ||
        catalog.videos.some((x) => x.videoUrl === mediaUrl)
      ) {
        console.log(`- update ${u.update_id}: already in catalog — skipping duplicate`)
        continue
      }

      const { title, artist } = songMeta(media, post)
      if (isVideo) {
        catalog.videos.unshift({
          title,
          artist,
          coverUrl,
          videoUrl: mediaUrl,
          durationSec: media.duration || 0
        })
        console.log(`+ Added video: ${artist ? artist + ' — ' : ''}${title}`)
      } else {
        catalog.songs.unshift({
          title,
          artist,
          coverUrl,
          audioUrl: mediaUrl,
          durationSec: media.duration || 0
        })
        console.log(`+ Added: ${artist ? artist + ' — ' : ''}${title}`)
      }
      added++
    } catch (e) {
      console.error('Failed on update', u.update_id, e.message)
    }
  }

  state.offset = maxUpdateId + 1
  fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2) + '\n')
  fs.writeFileSync(statePath, JSON.stringify(state, null, 2) + '\n')

  const changed = sh(`git -C ${WORKDIR} status --porcelain`)
  if (changed) {
    sh(`git -C ${WORKDIR} add catalog.json state.json manual-videos.json`)
    sh(`git -C ${WORKDIR} commit -m "sync: ${added} new item(s) from Telegram + manual videos"`)
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
