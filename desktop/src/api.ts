import md5 from 'blueimp-md5'

// Subsonic API client — mirrors the Android app's token auth so the desktop
// app connects to the very same server with the same credentials model.
const CLIENT = 'high-music-desktop'
const API_VERSION = '1.16.1'

export interface Creds {
  url: string
  username: string
  token: string
  salt: string
}

export interface Song {
  id: string
  title: string
  artist?: string
  album?: string
  albumId?: string
  artistId?: string
  coverArt?: string
  duration?: number
  track?: number
  year?: number
  genre?: string
  // Custom (user-added) songs play from this direct URL and use coverImageUrl
  // for artwork instead of the Subsonic getCoverArt endpoint
  directUrl?: string
  coverImageUrl?: string
}

export interface Album {
  id: string
  name: string
  artist?: string
  artistId?: string
  coverArt?: string
  songCount?: number
  duration?: number
  year?: number
  genre?: string
}

export interface Artist {
  id: string
  name: string
  coverArt?: string
  albumCount?: number
  artistImageUrl?: string
}

export interface Playlist {
  id: string
  name: string
  songCount?: number
  duration?: number
  coverArt?: string
  comment?: string
}

export function makeCreds(url: string, username: string, password: string): Creds {
  let clean = url.trim().replace(/\/+$/, '')
  if (!/^https?:\/\//i.test(clean)) clean = 'http://' + clean
  const salt = Math.random().toString(36).slice(2, 10)
  const token = md5(password + salt)
  return { url: clean, username, token, salt }
}

function authParams(c: Creds): URLSearchParams {
  return new URLSearchParams({
    u: c.username,
    t: c.token,
    s: c.salt,
    v: API_VERSION,
    c: CLIENT,
    f: 'json'
  })
}

export function apiUrl(
  c: Creds,
  path: string,
  params: Record<string, string | number | undefined> = {}
): string {
  const usp = authParams(c)
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null) usp.set(k, String(v))
  }
  return `${c.url}/rest/${path}?${usp.toString()}`
}

export async function call(
  c: Creds,
  path: string,
  params: Record<string, string | number | undefined> = {}
): Promise<any> {
  const res = await fetch(apiUrl(c, path, params))
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const json = await res.json()
  const sub = json['subsonic-response']
  if (!sub) throw new Error('Invalid response')
  if (sub.status !== 'ok') throw new Error(sub.error?.message || 'Server error')
  return sub
}

export function coverUrl(c: Creds | null, id?: string, size = 300): string {
  if (!c || !id) return ''
  return apiUrl(c, 'getCoverArt.view', { id, size })
}

export function streamUrl(c: Creds, id: string): string {
  return apiUrl(c, 'stream.view', { id })
}

// --- Typed helpers ---
export async function ping(c: Creds): Promise<boolean> {
  await call(c, 'ping.view')
  return true
}

export async function getRandomSongs(c: Creds, size = 30): Promise<Song[]> {
  const r = await call(c, 'getRandomSongs.view', { size })
  return r.randomSongs?.song ?? []
}

export async function getAlbumList(c: Creds, type = 'newest', size = 60): Promise<Album[]> {
  const r = await call(c, 'getAlbumList2.view', { type, size })
  return r.albumList2?.album ?? []
}

export async function getAlbum(c: Creds, id: string): Promise<{ album: Album; songs: Song[] }> {
  const r = await call(c, 'getAlbum.view', { id })
  const a = r.album
  return { album: a, songs: a?.song ?? [] }
}

export async function getArtists(c: Creds): Promise<Artist[]> {
  const r = await call(c, 'getArtists.view')
  const index = r.artists?.index ?? []
  return index.flatMap((i: any) => i.artist ?? [])
}

export async function getArtist(c: Creds, id: string): Promise<{ artist: Artist; albums: Album[] }> {
  const r = await call(c, 'getArtist.view', { id })
  const a = r.artist
  return { artist: a, albums: a?.album ?? [] }
}

export async function getTopSongs(c: Creds, artist: string, count = 20): Promise<Song[]> {
  const r = await call(c, 'getTopSongs.view', { artist, count })
  return r.topSongs?.song ?? []
}

export async function getGenres(c: Creds): Promise<string[]> {
  const r = await call(c, 'getGenres.view')
  const list = r.genres?.genre ?? []
  return list
    .map((g: any) => (typeof g === 'string' ? g : g.value || g.name))
    .filter(Boolean)
    .slice(0, 24)
}

export async function getSongsByGenre(c: Creds, genre: string, count = 20): Promise<Song[]> {
  const r = await call(c, 'getSongsByGenre.view', { genre, count })
  return r.songsByGenre?.song ?? []
}

export async function getPlaylists(c: Creds): Promise<Playlist[]> {
  const r = await call(c, 'getPlaylists.view')
  return r.playlists?.playlist ?? []
}

export async function getPlaylist(c: Creds, id: string): Promise<{ playlist: Playlist; songs: Song[] }> {
  const r = await call(c, 'getPlaylist.view', { id })
  const p = r.playlist
  return { playlist: p, songs: p?.entry ?? [] }
}

export interface SearchResult {
  artists: Artist[]
  albums: Album[]
  songs: Song[]
}

export async function search(c: Creds, query: string): Promise<SearchResult> {
  const r = await call(c, 'search3.view', {
    query,
    artistCount: 20,
    albumCount: 30,
    songCount: 50
  })
  const s = r.searchResult3 ?? {}
  return {
    artists: s.artist ?? [],
    albums: s.album ?? [],
    songs: s.song ?? []
  }
}

export async function getStarred(c: Creds): Promise<{ songs: Song[]; albums: Album[]; artists: Artist[] }> {
  const r = await call(c, 'getStarred2.view')
  const s = r.starred2 ?? {}
  return { songs: s.song ?? [], albums: s.album ?? [], artists: s.artist ?? [] }
}

export async function createPlaylist(c: Creds, name: string, songIds: string[]): Promise<void> {
  const usp = authParams(c)
  usp.set('name', name)
  for (const id of songIds) usp.append('songId', id)
  const res = await fetch(`${c.url}/rest/createPlaylist.view?${usp.toString()}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const json = await res.json()
  const sub = json['subsonic-response']
  if (!sub || sub.status !== 'ok') throw new Error(sub?.error?.message || 'Server error')
}

export async function star(c: Creds, id: string): Promise<void> {
  await call(c, 'star.view', { id })
}

export async function unstar(c: Creds, id: string): Promise<void> {
  await call(c, 'unstar.view', { id })
}

export function fmtDuration(sec?: number): string {
  if (!sec || sec < 0) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}
