import { Song } from './api'

// Shared online catalog: an admin curates a Telegram channel; every client reads
// the pinned catalog.json document and plays via the shared bot. No secret is
// bundled — config is entered by the admin or fetched from a remote config URL.
const CFG_KEY = 'highmusic.catalog.cfg'

export interface CatalogConfig {
  token: string
  channel: string
  configUrl?: string
}

export interface CatalogSong {
  fileId: string
  title: string
  artist: string
  coverUrl: string
  durationSec: number
}

export function loadCatalogConfig(): CatalogConfig | null {
  try {
    const raw = localStorage.getItem(CFG_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function saveCatalogConfig(cfg: CatalogConfig | null) {
  if (cfg) localStorage.setItem(CFG_KEY, JSON.stringify(cfg))
  else localStorage.removeItem(CFG_KEY)
}

const api = (token: string, method: string) => `https://api.telegram.org/bot${token}/${method}`

// Regular users: fetch { botToken, channelId } from a URL the admin hosts
export async function configFromUrl(url: string): Promise<CatalogConfig> {
  const j = await fetch(url.trim()).then((r) => r.json())
  const token = j.botToken || j.token
  const channel = j.channelId || j.channel
  if (!token || !channel) throw new Error('پیکربندی نامعتبر است')
  const cfg = { token, channel, configUrl: url.trim() }
  saveCatalogConfig(cfg)
  return cfg
}

export async function fetchCatalog(cfg: CatalogConfig): Promise<CatalogSong[]> {
  const chat = await fetch(
    api(cfg.token, 'getChat') + `?chat_id=${encodeURIComponent(cfg.channel)}`
  ).then((r) => r.json())
  if (!chat.ok) throw new Error('دسترسی به کانال ممکن نشد')
  const docFileId = chat.result?.pinned_message?.document?.file_id
  if (!docFileId) return []
  const file = await fetch(api(cfg.token, 'getFile') + `?file_id=${docFileId}`).then((r) => r.json())
  const path = file.result.file_path
  const json = await fetch(`https://api.telegram.org/file/bot${cfg.token}/${path}`).then((r) =>
    r.json()
  )
  return (json.songs || []) as CatalogSong[]
}

export async function resolveCatalogUrl(cfg: CatalogConfig, fileId: string): Promise<string> {
  const file = await fetch(api(cfg.token, 'getFile') + `?file_id=${fileId}`).then((r) => r.json())
  if (!file.ok) throw new Error('پخش ناموفق (فایل بزرگ‌تر از ۲۰ مگابایت؟)')
  return `https://api.telegram.org/file/bot${cfg.token}/${file.result.file_path}`
}

// Admin: upload a song to the channel and republish + pin the catalog
export async function adminAddSong(
  cfg: CatalogConfig,
  existing: CatalogSong[],
  file: File,
  title: string,
  artist: string,
  coverUrl: string
): Promise<CatalogSong[]> {
  const audioForm = new FormData()
  audioForm.append('chat_id', cfg.channel)
  audioForm.append('title', title)
  audioForm.append('performer', artist)
  audioForm.append('audio', file, (title || 'audio') + '.mp3')
  const audioRes = await fetch(api(cfg.token, 'sendAudio'), { method: 'POST', body: audioForm }).then(
    (r) => r.json()
  )
  if (!audioRes.ok) throw new Error(audioRes.description || 'آپلود ناموفق')
  const audio = audioRes.result.audio || audioRes.result.document

  const newSong: CatalogSong = {
    fileId: audio.file_id,
    title: title || audio.file_name || 'آهنگ',
    artist,
    coverUrl,
    durationSec: audio.duration || 0
  }
  const updated = [newSong, ...existing]

  const blob = new Blob([JSON.stringify({ songs: updated })], { type: 'application/json' })
  const docForm = new FormData()
  docForm.append('chat_id', cfg.channel)
  docForm.append('document', blob, 'catalog.json')
  const docRes = await fetch(api(cfg.token, 'sendDocument'), { method: 'POST', body: docForm }).then(
    (r) => r.json()
  )
  if (!docRes.ok) throw new Error(docRes.description || 'انتشار کاتالوگ ناموفق')

  const msgId = docRes.result.message_id
  await fetch(
    api(cfg.token, 'pinChatMessage') +
      `?chat_id=${encodeURIComponent(cfg.channel)}&message_id=${msgId}&disable_notification=true`
  )
  return updated
}

export function catalogToSong(s: CatalogSong, directUrl: string): Song {
  return {
    id: 'tg_' + s.fileId,
    title: s.title,
    artist: s.artist,
    directUrl,
    coverImageUrl: s.coverUrl,
    duration: s.durationSec
  }
}
