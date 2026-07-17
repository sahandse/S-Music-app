import { Song } from './api'

// Shared online catalog: a public catalog.json (kept up to date by a GitHub
// Actions worker from the Telegram channel) with direct audio URLs. The client
// only reads a public JSON and plays direct links — no token, no admin logic.
const URL_KEY = 'highmusic.catalog.url'

const DEFAULT_CATALOG_URL =
  'https://raw.githubusercontent.com/sahandse/himusic-catalog/main/catalog.json'

export interface CatalogSong {
  title: string
  artist: string
  coverUrl: string
  audioUrl: string
  durationSec: number
}

export function catalogUrl(): string {
  return localStorage.getItem(URL_KEY) || DEFAULT_CATALOG_URL
}

export function setCatalogUrl(url: string) {
  if (url.trim()) localStorage.setItem(URL_KEY, url.trim())
  else localStorage.removeItem(URL_KEY)
}

export async function fetchCatalog(): Promise<CatalogSong[]> {
  const res = await fetch(catalogUrl() + '?t=' + Date.now())
  if (!res.ok) throw new Error('خطا در دریافت کاتالوگ')
  const json = await res.json()
  const seen = new Set<string>()
  return (json.songs || []).filter((s: any) => {
    if (!s || !s.audioUrl || seen.has(s.audioUrl)) return false
    seen.add(s.audioUrl)
    return true
  }) as CatalogSong[]
}

export function catalogToSong(s: CatalogSong): Song {
  return {
    id: 'cat_' + s.audioUrl,
    title: s.title,
    artist: s.artist,
    directUrl: s.audioUrl,
    coverImageUrl: s.coverUrl,
    duration: s.durationSec
  }
}
