import { Song } from './api'

// User-added songs: cover URL + artist + title + a direct audio link.
// Stored locally so they persist across restarts and don't need a server.
const KEY = 'highmusic.customSongs'

export interface CustomSong {
  id: string
  title: string
  artist: string
  coverImageUrl: string
  directUrl: string
  addedAt: number
}

export function loadCustomSongs(): CustomSong[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function save(list: CustomSong[]) {
  localStorage.setItem(KEY, JSON.stringify(list))
}

export function addCustomSong(input: Omit<CustomSong, 'id' | 'addedAt'>): CustomSong[] {
  const list = loadCustomSongs()
  const item: CustomSong = {
    ...input,
    id: 'custom_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
    addedAt: Date.now()
  }
  const next = [item, ...list]
  save(next)
  return next
}

export function removeCustomSong(id: string): CustomSong[] {
  const next = loadCustomSongs().filter((s) => s.id !== id)
  save(next)
  return next
}

// Bulk import from a pasted JSON array (for adding many at once)
export function importCustomSongs(json: string): CustomSong[] {
  const parsed = JSON.parse(json)
  if (!Array.isArray(parsed)) throw new Error('باید یک آرایه JSON باشد')
  const list = loadCustomSongs()
  const added: CustomSong[] = parsed
    .filter((o: any) => o && o.title && o.directUrl)
    .map((o: any) => ({
      id: 'custom_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
      title: String(o.title),
      artist: String(o.artist || ''),
      coverImageUrl: String(o.coverImageUrl || o.cover || ''),
      directUrl: String(o.directUrl || o.url),
      addedAt: Date.now()
    }))
  const next = [...added, ...list]
  save(next)
  return next
}

export function toSong(c: CustomSong): Song {
  return {
    id: c.id,
    title: c.title,
    artist: c.artist,
    directUrl: c.directUrl,
    coverImageUrl: c.coverImageUrl
  }
}
