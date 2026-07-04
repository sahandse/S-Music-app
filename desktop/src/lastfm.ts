import md5 from 'blueimp-md5'
import { getSettings, setSettings } from './settings'

// Minimal Last.fm scrobbler. The user supplies their own API key + secret
// (from last.fm/api/account/create) and logs in with their Last.fm
// username/password once to obtain a session key (mobile-session auth).
const ROOT = 'https://ws.audioscrobbler.com/2.0/'

function sign(params: Record<string, string>, secret: string): string {
  const keys = Object.keys(params).sort()
  let s = ''
  for (const k of keys) s += k + params[k]
  return md5(s + secret)
}

async function post(params: Record<string, string>, secret: string): Promise<any> {
  const sig = sign(params, secret)
  const body = new URLSearchParams({ ...params, api_sig: sig, format: 'json' })
  const res = await fetch(ROOT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString()
  })
  return res.json()
}

// Log in and store the session key. Returns the Last.fm username on success.
export async function lastfmLogin(username: string, password: string): Promise<string> {
  const s = getSettings()
  if (!s.lastfmKey || !s.lastfmSecret) throw new Error('کلید API و Secret را وارد کنید')
  const params = {
    method: 'auth.getMobileSession',
    api_key: s.lastfmKey,
    username,
    password
  }
  const j = await post(params, s.lastfmSecret)
  if (j.error || !j.session?.key) throw new Error(j.message || 'ورود ناموفق')
  setSettings({ lastfmSession: j.session.key, lastfmUser: j.session.name, lastfmEnabled: true })
  return j.session.name
}

export function lastfmLogout() {
  setSettings({ lastfmSession: '', lastfmUser: '', lastfmEnabled: false })
}

function ready(): { key: string; secret: string; sk: string } | null {
  const s = getSettings()
  if (!s.lastfmEnabled || !s.lastfmSession || !s.lastfmKey || !s.lastfmSecret) return null
  return { key: s.lastfmKey, secret: s.lastfmSecret, sk: s.lastfmSession }
}

export async function nowPlaying(title: string, artist: string, album?: string) {
  const r = ready()
  if (!r || !title || !artist) return
  try {
    await post(
      {
        method: 'track.updateNowPlaying',
        api_key: r.key,
        sk: r.sk,
        artist,
        track: title,
        ...(album ? { album } : {})
      },
      r.secret
    )
  } catch {
    /* best-effort */
  }
}

export async function scrobble(title: string, artist: string, album?: string) {
  const r = ready()
  if (!r || !title || !artist) return
  try {
    await post(
      {
        method: 'track.scrobble',
        api_key: r.key,
        sk: r.sk,
        artist,
        track: title,
        timestamp: String(Math.floor(Date.now() / 1000)),
        ...(album ? { album } : {})
      },
      r.secret
    )
  } catch {
    /* best-effort */
  }
}
