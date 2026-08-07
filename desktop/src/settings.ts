// User-configurable desktop settings, persisted in localStorage. A tiny
// pub/sub lets components react to changes without a heavier state library.

export type ThemeMode = 'dark' | 'light'
export type Lang = 'fa' | 'en'

export interface Settings {
  theme: ThemeMode
  lang: Lang
  accent: string
  fadeOnPause: boolean
  crossfadeSec: number // 0 = off
  eqEnabled: boolean
  eqBands: number[] // gain dB per band, see EQ_FREQS
  downloadFolder: string
}

export const EQ_FREQS = [60, 230, 910, 3600, 14000]

const KEY = 'highmusic.settings'

const DEFAULTS: Settings = {
  theme: 'dark',
  lang: 'fa',
  accent: '#e0264e',
  fadeOnPause: true,
  crossfadeSec: 0,
  eqEnabled: false,
  eqBands: [0, 0, 0, 0, 0],
  downloadFolder: ''
}

let current: Settings = load()
const listeners = new Set<(s: Settings) => void>()

function load(): Settings {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? { ...DEFAULTS, ...JSON.parse(raw) } : { ...DEFAULTS }
  } catch {
    return { ...DEFAULTS }
  }
}

export function getSettings(): Settings {
  return current
}

export function setSettings(patch: Partial<Settings>) {
  current = { ...current, ...patch }
  localStorage.setItem(KEY, JSON.stringify(current))
  applyTheme(current)
  listeners.forEach((l) => l(current))
}

export function onSettings(cb: (s: Settings) => void): () => void {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

// Reflect theme + accent + text direction onto the document root so the whole
// UI updates live (CSS variables + rtl/ltr for the language switch).
export function applyTheme(s: Settings = current) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', s.theme)
  document.documentElement.setAttribute('dir', s.lang === 'en' ? 'ltr' : 'rtl')
  document.documentElement.setAttribute('lang', s.lang)
  document.documentElement.style.setProperty('--accent', s.accent)
}

export function exportSettings(): string {
  return JSON.stringify({
    version: 1,
    settings: current,
    customSongs: (() => {
      try {
        const raw = localStorage.getItem('highmusic.customSongs')
        return raw ? JSON.parse(raw) : []
      } catch {
        return []
      }
    })()
  })
}

export function importSettings(json: string): boolean {
  try {
    const data = JSON.parse(json)
    if (!data.settings) return false
    setSettings(data.settings)
    if (Array.isArray(data.customSongs)) {
      localStorage.setItem('highmusic.customSongs', JSON.stringify(data.customSongs))
    }
    return true
  } catch {
    return false
  }
}
