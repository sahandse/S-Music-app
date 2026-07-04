import { Song } from './api'

// Podcasts via the free iTunes Search API (no key) + the show's RSS feed.
// Episodes play through the normal player using their direct enclosure URL.

export interface Podcast {
  id: number
  title: string
  author: string
  artwork: string
  feedUrl: string
}

export interface Episode {
  title: string
  audioUrl: string
  durationSec: number
  date: string
  artwork: string
  podcastTitle: string
}

export async function searchPodcasts(term: string): Promise<Podcast[]> {
  const u = new URL('https://itunes.apple.com/search')
  u.searchParams.set('media', 'podcast')
  u.searchParams.set('limit', '30')
  u.searchParams.set('term', term)
  const res = await fetch(u.toString())
  if (!res.ok) throw new Error('search failed')
  const j = await res.json()
  return (j.results || [])
    .filter((r: any) => r.feedUrl)
    .map((r: any) => ({
      id: r.collectionId,
      title: r.collectionName,
      author: r.artistName,
      artwork: r.artworkUrl600 || r.artworkUrl100 || '',
      feedUrl: r.feedUrl
    }))
}

function parseDuration(s: string | null): number {
  if (!s) return 0
  if (/^\d+$/.test(s)) return parseInt(s)
  const parts = s.split(':').map((p) => parseInt(p) || 0)
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2]
  if (parts.length === 2) return parts[0] * 60 + parts[1]
  return 0
}

export async function fetchEpisodes(pod: Podcast): Promise<Episode[]> {
  const res = await fetch(pod.feedUrl)
  if (!res.ok) throw new Error('feed failed')
  const text = await res.text()
  const doc = new DOMParser().parseFromString(text, 'text/xml')
  const channelImg =
    doc.querySelector('channel > image > url')?.textContent ||
    doc.querySelector('channel > *|image')?.getAttribute('href') ||
    pod.artwork
  const items = Array.from(doc.querySelectorAll('item'))
  const out: Episode[] = []
  for (const it of items.slice(0, 60)) {
    const enclosure = it.querySelector('enclosure')
    const audioUrl = enclosure?.getAttribute('url') || ''
    if (!audioUrl) continue
    const title = it.querySelector('title')?.textContent || 'Episode'
    const durEl =
      it.getElementsByTagName('itunes:duration')[0] || it.querySelector('duration')
    const date = it.querySelector('pubDate')?.textContent || ''
    const epImg =
      it.getElementsByTagName('itunes:image')[0]?.getAttribute('href') || channelImg || ''
    out.push({
      title: title.trim(),
      audioUrl,
      durationSec: parseDuration(durEl?.textContent || durEl?.getAttribute?.('href') || null),
      date: date ? new Date(date).toLocaleDateString() : '',
      artwork: epImg,
      podcastTitle: pod.title
    })
  }
  return out
}

export function episodeToSong(e: Episode): Song {
  return {
    id: 'pod_' + e.audioUrl,
    title: e.title,
    artist: e.podcastTitle,
    directUrl: e.audioUrl,
    coverImageUrl: e.artwork,
    duration: e.durationSec
  }
}
