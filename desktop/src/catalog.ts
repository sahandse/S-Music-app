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
  return (json.songs || []).filter((s: any) => s && s.audioUrl) as CatalogSong[]
}

// --- Admin-only manual sync ---------------------------------------------
// Triggers the GitHub Actions catalog-sync workflow on demand. Uses a
// fine-grained token entered by the admin and kept only in local storage
// (never embedded in the app), so ordinary users never have it.
const GH_TOKEN_KEY = 'highmusic.ghToken'
const SYNC_WORKFLOW =
  'https://api.github.com/repos/sahandse/S-Music-app/actions/workflows/catalog-sync.yml/dispatches'
const SYNC_REF = 'claude/music-app-fonts-q1foo3'

export function ghToken(): string {
  return localStorage.getItem(GH_TOKEN_KEY) || ''
}
export function setGhToken(t: string) {
  const v = t.trim()
  if (v) localStorage.setItem(GH_TOKEN_KEY, v)
  else localStorage.removeItem(GH_TOKEN_KEY)
}

export async function dispatchSync(): Promise<void> {
  const token = ghToken()
  if (!token) throw new Error('no-token')
  const res = await fetch(SYNC_WORKFLOW, {
    method: 'POST',
    headers: {
      Authorization: 'Bearer ' + token,
      Accept: 'application/vnd.github+json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ ref: SYNC_REF })
  })
  if (res.status !== 204) {
    throw new Error('HTTP ' + res.status + ' ' + (await res.text()))
  }
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
