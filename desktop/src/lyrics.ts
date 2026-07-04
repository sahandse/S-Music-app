// Fetch plain/synced lyrics from LRCLIB (no key), matching the Android client.

export interface LyricLine {
  time: number // seconds; -1 for unsynced
  text: string
}

const cache = new Map<string, LyricLine[]>()

function parseLrc(lrc: string): LyricLine[] {
  const out: LyricLine[] = []
  for (const raw of lrc.split('\n')) {
    const m = raw.match(/^((?:\[\d{1,2}:\d{2}(?:\.\d{1,3})?\])+)(.*)$/)
    if (!m) continue
    const text = m[2].trim()
    const stamps = m[1].match(/\[(\d{1,2}):(\d{2}(?:\.\d{1,3})?)\]/g) || []
    for (const st of stamps) {
      const mm = st.match(/\[(\d{1,2}):(\d{2}(?:\.\d{1,3})?)\]/)
      if (!mm) continue
      const t = parseInt(mm[1]) * 60 + parseFloat(mm[2])
      out.push({ time: t, text })
    }
  }
  out.sort((a, b) => a.time - b.time)
  return out
}

export async function fetchLyrics(title: string, artist: string): Promise<LyricLine[]> {
  const key = (artist + '|' + title).toLowerCase()
  if (cache.has(key)) return cache.get(key)!
  try {
    const u = new URL('https://lrclib.net/api/get')
    u.searchParams.set('track_name', title)
    u.searchParams.set('artist_name', artist || '')
    const res = await fetch(u.toString())
    if (!res.ok) throw new Error('no lyrics')
    const j = await res.json()
    let lines: LyricLine[]
    if (j.syncedLyrics) lines = parseLrc(j.syncedLyrics)
    else if (j.plainLyrics)
      lines = String(j.plainLyrics)
        .split('\n')
        .map((t: string) => ({ time: -1, text: t }))
    else lines = []
    cache.set(key, lines)
    return lines
  } catch {
    cache.set(key, [])
    return []
  }
}
