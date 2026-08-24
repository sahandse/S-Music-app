import { catalogUrl } from './catalog'

// Music videos: merges the admin-curated manual-videos.json with the
// Telegram-scraped "videos" array already inside catalog.json. Same public,
// unauthenticated JSON the Android app reads — manual entries win on a
// duplicate videoUrl since their metadata (artist/genre) is hand-cleaned.
const MANUAL_VIDEOS_URL =
  'https://raw.githubusercontent.com/sahandse/himusic-catalog/main/manual-videos.json'

export interface MusicVideoItem {
  title: string
  artist: string
  genre: string
  coverUrl: string
  videoUrl: string
  durationSec: number
}

async function fetchVideosArray(url: string): Promise<MusicVideoItem[]> {
  try {
    const res = await fetch(url + '?t=' + Date.now())
    if (!res.ok) return []
    const json = await res.json()
    return ((json.videos || []) as any[])
      .filter((v) => v && typeof v.videoUrl === 'string' && v.videoUrl.trim())
      .map((v) => ({
        title: v.title || '',
        artist: v.artist || '',
        genre: v.genre || '',
        coverUrl: v.coverUrl || '',
        videoUrl: v.videoUrl,
        durationSec: v.durationSec || 0
      }))
  } catch {
    return []
  }
}

export async function fetchMusicVideos(): Promise<MusicVideoItem[]> {
  const [manual, auto] = await Promise.all([
    fetchVideosArray(MANUAL_VIDEOS_URL),
    fetchVideosArray(catalogUrl())
  ])
  const seen = new Set<string>()
  const merged: MusicVideoItem[] = []
  for (const v of [...manual, ...auto]) {
    if (seen.has(v.videoUrl)) continue
    seen.add(v.videoUrl)
    merged.push(v)
  }
  return merged
}

export function fmtDuration(sec: number): string {
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}
