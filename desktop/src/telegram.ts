import { Song } from './api'

// Telegram as free unlimited cloud storage via the user's own bot.
// Token + chat id live in localStorage; the song list (fileId + metadata) too.
const CFG_KEY = 'highmusic.telegram.cfg'
const SONGS_KEY = 'highmusic.telegram.songs'

export interface TgConfig {
  token: string
  chatId: string
}

export interface TgSong {
  fileId: string
  title: string
  artist: string
  coverUrl: string
  durationSec: number
  sizeBytes: number
}

export function loadTgConfig(): TgConfig | null {
  try {
    const raw = localStorage.getItem(CFG_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function saveTgConfig(cfg: TgConfig | null) {
  if (cfg) localStorage.setItem(CFG_KEY, JSON.stringify(cfg))
  else localStorage.removeItem(CFG_KEY)
}

export function loadTgSongs(): TgSong[] {
  try {
    const raw = localStorage.getItem(SONGS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveTgSongs(list: TgSong[]) {
  localStorage.setItem(SONGS_KEY, JSON.stringify(list))
}

const api = (token: string, method: string) =>
  `https://api.telegram.org/bot${token}/${method}`

// Validate token and auto-discover the chat id from recent updates.
// The user must send /start (or any message) to their bot first.
export async function connectTelegram(token: string): Promise<TgConfig> {
  const meRes = await fetch(api(token, 'getMe')).then((r) => r.json())
  if (!meRes.ok) throw new Error('توکن بات نامعتبر است')
  const upd = await fetch(api(token, 'getUpdates')).then((r) => r.json())
  const results: any[] = upd.result || []
  let chatId = ''
  for (let i = results.length - 1; i >= 0; i--) {
    const msg = results[i].message || results[i].channel_post
    const id = msg?.chat?.id
    if (id) {
      chatId = String(id)
      break
    }
  }
  if (!chatId) {
    throw new Error('ابتدا در تلگرام به بات خود /start بدهید، سپس دوباره وصل شوید')
  }
  const cfg = { token: token.trim(), chatId }
  saveTgConfig(cfg)
  return cfg
}

export async function uploadTgSong(
  cfg: TgConfig,
  file: File,
  title: string,
  artist: string,
  coverUrl: string
): Promise<TgSong[]> {
  const form = new FormData()
  form.append('chat_id', cfg.chatId)
  form.append('title', title)
  form.append('performer', artist)
  form.append('audio', file, (title || 'audio') + '.mp3')

  const res = await fetch(api(cfg.token, 'sendAudio'), { method: 'POST', body: form }).then((r) =>
    r.json()
  )
  if (!res.ok) throw new Error(res.description || 'آپلود ناموفق')
  const audio = res.result.audio || res.result.document
  if (!audio) throw new Error('پاسخ نامعتبر تلگرام')

  const song: TgSong = {
    fileId: audio.file_id,
    title: title || audio.file_name || 'آهنگ',
    artist,
    coverUrl,
    durationSec: audio.duration || 0,
    sizeBytes: audio.file_size || file.size
  }
  const next = [song, ...loadTgSongs()]
  saveTgSongs(next)
  return next
}

export function removeTgSong(fileId: string): TgSong[] {
  const next = loadTgSongs().filter((s) => s.fileId !== fileId)
  saveTgSongs(next)
  return next
}

// Telegram file links expire, so resolve a fresh one at play time
export async function resolveTgUrl(cfg: TgConfig, fileId: string): Promise<string> {
  const res = await fetch(api(cfg.token, 'getFile') + `?file_id=${fileId}`).then((r) => r.json())
  if (!res.ok) throw new Error('دریافت فایل ناموفق (شاید بزرگ‌تر از ۲۰ مگابایت است)')
  return `https://api.telegram.org/file/bot${cfg.token}/${res.result.file_path}`
}

export function tgToSong(s: TgSong, directUrl: string): Song {
  return {
    id: 'tg_' + s.fileId,
    title: s.title,
    artist: s.artist,
    directUrl,
    coverImageUrl: s.coverUrl,
    duration: s.durationSec
  }
}
