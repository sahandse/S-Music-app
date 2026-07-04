// Artist biography from the Wikipedia REST summary endpoint (no key).
// Tries the UI language first, then falls back to English.
import { getLang } from './i18n'

const cache = new Map<string, string>()

async function trySummary(lang: string, title: string): Promise<string | null> {
  try {
    const u = `https://${lang}.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(title)}`
    const res = await fetch(u)
    if (!res.ok) return null
    const j = await res.json()
    if (j.type === 'disambiguation') return null
    return j.extract || null
  } catch {
    return null
  }
}

export async function fetchArtistBio(name: string): Promise<string> {
  if (!name) return ''
  const key = name.toLowerCase()
  if (cache.has(key)) return cache.get(key)!
  const lang = getLang()
  let bio =
    (await trySummary(lang, name)) ||
    (await trySummary(lang, name + (lang === 'fa' ? ' (خواننده)' : ' (musician)'))) ||
    (lang !== 'en' ? await trySummary('en', name) : null) ||
    ''
  cache.set(key, bio)
  return bio
}
