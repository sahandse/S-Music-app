import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react'
import { Creds, Song, streamUrl, coverUrl } from './api'
import { AudioGraph } from './audioGraph'
import { getSettings, onSettings, applyTheme, Settings } from './settings'
import { bridge } from './desktop'

const CREDS_KEY = 'highmusic.creds'

type RepeatMode = 'off' | 'all' | 'one'

interface AppState {
  creds: Creds | null
  setCreds: (c: Creds | null) => void
  logout: () => void

  view: string
  param: string | null
  navigate: (view: string, param?: string | null) => void

  queue: Song[]
  index: number
  current: Song | null
  isPlaying: boolean
  progress: number
  duration: number
  volume: number
  shuffle: boolean
  repeat: RepeatMode

  playQueue: (songs: Song[], startIndex?: number) => void
  playNow: (song: Song) => void
  addToQueue: (songs: Song[]) => void
  removeFromQueue: (i: number) => void
  reorderQueue: (from: number, to: number) => void
  togglePlay: () => void
  next: () => void
  prev: () => void
  seek: (sec: number) => void
  setVolume: (v: number) => void
  toggleShuffle: () => void
  cycleRepeat: () => void
  jumpTo: (i: number) => void

  settings: Settings
  fullscreen: boolean
  setFullscreen: (v: boolean) => void
  mini: boolean
}

const Ctx = createContext<AppState | null>(null)

export function useApp(): AppState {
  const v = useContext(Ctx)
  if (!v) throw new Error('useApp outside provider')
  return v
}

function srcFor(creds: Creds | null, song: Song | null): string {
  if (!song) return ''
  if (song.directUrl) return song.directUrl
  return creds ? streamUrl(creds, song.id) : ''
}

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [creds, setCredsState] = useState<Creds | null>(() => {
    try {
      const raw = localStorage.getItem(CREDS_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  })

  const [view, setView] = useState('home')
  const [param, setParam] = useState<string | null>(null)

  const [queue, setQueue] = useState<Song[]>([])
  const [index, setIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [progress, setProgress] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolumeState] = useState(() => {
    const v = parseFloat(localStorage.getItem('highmusic.volume') || '1')
    return isNaN(v) ? 1 : v
  })
  const [shuffle, setShuffle] = useState(false)
  const [repeat, setRepeat] = useState<RepeatMode>('off')

  const [settings, setSettingsState] = useState<Settings>(() => getSettings())
  const [fullscreen, setFullscreen] = useState(false)
  const [mini, setMini] = useState(false)

  const audioRef = useRef<HTMLAudioElement | null>(null)
  const graphRef = useRef<AudioGraph | null>(null)
  if (!audioRef.current && typeof Audio !== 'undefined') {
    audioRef.current = new Audio()
    audioRef.current.crossOrigin = 'anonymous'
  }

  // Keep latest values available to event handlers without re-binding them
  const stateRef = useRef({ shuffle, repeat, queue, index, volume, settings, creds })
  stateRef.current = { shuffle, repeat, queue, index, volume, settings, creds }

  const current = queue[index] ?? null

  const transitioningRef = useRef(false)

  const setCreds = useCallback((c: Creds | null) => {
    setCredsState(c)
    if (c) localStorage.setItem(CREDS_KEY, JSON.stringify(c))
    else localStorage.removeItem(CREDS_KEY)
  }, [])

  const navigate = useCallback((v: string, p: string | null = null) => {
    setView(v)
    setParam(p)
  }, [])

  // Apply theme once + subscribe to settings changes
  useEffect(() => {
    applyTheme(getSettings())
    return onSettings((s) => {
      setSettingsState(s)
      graphRef.current?.setEqEnabled(s.eqEnabled)
      graphRef.current?.setEqGains(s.eqBands)
    })
  }, [])

  // Load & play whatever song is at `index`
  useEffect(() => {
    const audio = audioRef.current
    if (!audio || !current) return
    const src = srcFor(creds, current)
    if (!src) return

    audio.src = src
    if (!graphRef.current) graphRef.current = new AudioGraph(audio)
    const g = graphRef.current
    g.resume()
    g.setEqEnabled(settings.eqEnabled)
    g.setEqGains(settings.eqBands)

    const cf = settings.crossfadeSec
    if (cf > 0) g.setVolume(0)
    else g.setVolume(volume)

    audio.play()
      .then(() => {
        setIsPlaying(true)
        if (cf > 0) g.fadeTo(volume, Math.min(cf, 2))
      })
      .catch(() => setIsPlaying(false))
    transitioningRef.current = false
    updateMediaSession(current)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index, queue, creds])

  // Audio element event wiring (bound once; reads live state via refs)
  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return
    let lastTime = 0
    const onTime = () => {
      setProgress(audio.currentTime)
      lastTime = audio.currentTime
      updatePosition(audio)
      handleCrossfade(audio)
    }
    const onDur = () => setDuration(audio.duration || 0)
    const onPlay = () => setIsPlaying(true)
    const onPause = () => setIsPlaying(false)
    const onEnded = () => handleEnded()
    audio.addEventListener('timeupdate', onTime)
    audio.addEventListener('durationchange', onDur)
    audio.addEventListener('play', onPlay)
    audio.addEventListener('pause', onPause)
    audio.addEventListener('ended', onEnded)
    return () => {
      audio.removeEventListener('timeupdate', onTime)
      audio.removeEventListener('durationchange', onDur)
      audio.removeEventListener('play', onPlay)
      audio.removeEventListener('pause', onPause)
      audio.removeEventListener('ended', onEnded)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleCrossfade = (audio: HTMLAudioElement) => {
    const { settings: s } = stateRef.current
    if (s.crossfadeSec <= 0 || transitioningRef.current) return
    const remaining = (audio.duration || 0) - audio.currentTime
    if (audio.duration && remaining > 0 && remaining <= s.crossfadeSec) {
      transitioningRef.current = true
      graphRef.current?.fadeTo(0.0001, Math.min(s.crossfadeSec, remaining))
    }
  }

  const handleEnded = () => {
    const { repeat: rp } = stateRef.current
    if (rp === 'one') {
      const audio = audioRef.current
      if (audio) {
        audio.currentTime = 0
        audio.play()
      }
      return
    }
    goNext(true)
  }

  const goNext = (auto = false) => {
    const { shuffle: sh, repeat: rp, queue: q } = stateRef.current
    setIndex((i) => {
      if (sh && q.length > 1) {
        let r = i
        while (r === i) r = Math.floor(Math.random() * q.length)
        return r
      }
      if (i < q.length - 1) return i + 1
      if (rp === 'all') return 0
      return i
    })
  }

  // --- MediaSession (Windows SMTC / OS media overlay) ---
  const updateMediaSession = (song: Song) => {
    if (typeof navigator === 'undefined' || !('mediaSession' in navigator)) return
    const art = song.directUrl
      ? song.coverImageUrl || ''
      : coverUrl(stateRef.current.creds, song.coverArt, 512)
    try {
      // @ts-ignore
      navigator.mediaSession.metadata = new MediaMetadata({
        title: song.title,
        artist: song.artist || '',
        album: song.album || 'High Music',
        artwork: art ? [{ src: art, sizes: '512x512', type: 'image/jpeg' }] : []
      })
    } catch {
      /* ignore */
    }
  }

  const updatePosition = (audio: HTMLAudioElement) => {
    if (typeof navigator === 'undefined' || !('mediaSession' in navigator)) return
    try {
      if (audio.duration && isFinite(audio.duration)) {
        // @ts-ignore
        navigator.mediaSession.setPositionState({
          duration: audio.duration,
          position: Math.min(audio.currentTime, audio.duration),
          playbackRate: audio.playbackRate
        })
      }
    } catch {
      /* ignore */
    }
  }

  useEffect(() => {
    if (typeof navigator === 'undefined' || !('mediaSession' in navigator)) return
    const ms = navigator.mediaSession
    ms.setActionHandler('play', () => togglePlayRef.current())
    ms.setActionHandler('pause', () => togglePlayRef.current())
    ms.setActionHandler('nexttrack', () => goNext(false))
    ms.setActionHandler('previoustrack', () => prevRef.current())
    try {
      ms.setActionHandler('seekto', (d: any) => {
        if (d.seekTime != null && audioRef.current) audioRef.current.currentTime = d.seekTime
      })
    } catch {
      /* not all browsers */
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (typeof navigator !== 'undefined' && 'mediaSession' in navigator) {
      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused'
    }
  }, [isPlaying])

  // --- Desktop bridge: media keys / tray + mini mode ---
  useEffect(() => {
    const b = bridge()
    if (!b) return
    const off1 = b.onMediaControl((action) => {
      if (action === 'playpause') togglePlayRef.current()
      else if (action === 'next') goNext(false)
      else if (action === 'prev') prevRef.current()
      else if (action === 'pause') audioRef.current?.pause()
    })
    const off2 = b.onMiniMode((on) => setMini(on))
    return () => {
      off1 && off1()
      off2 && off2()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const playQueue = useCallback((songs: Song[], startIndex = 0) => {
    if (!songs.length) return
    setQueue(songs)
    setIndex(startIndex)
  }, [])

  const playNow = useCallback((song: Song) => {
    setQueue([song])
    setIndex(0)
  }, [])

  const addToQueue = useCallback((songs: Song[]) => {
    setQueue((q) => [...q, ...songs])
  }, [])

  const removeFromQueue = useCallback((i: number) => {
    setQueue((q) => q.filter((_, idx) => idx !== i))
    setIndex((cur) => (i < cur ? cur - 1 : cur))
  }, [])

  const reorderQueue = useCallback((from: number, to: number) => {
    setQueue((q) => {
      if (from === to || from < 0 || to < 0 || from >= q.length || to >= q.length) return q
      const arr = q.slice()
      const [item] = arr.splice(from, 1)
      arr.splice(to, 0, item)
      return arr
    })
    setIndex((cur) => {
      if (from === cur) return to
      if (from < cur && to >= cur) return cur - 1
      if (from > cur && to <= cur) return cur + 1
      return cur
    })
  }, [])

  const togglePlay = useCallback(() => {
    const audio = audioRef.current
    if (!audio || !current) return
    const g = graphRef.current
    const s = stateRef.current.settings
    if (audio.paused) {
      audio.play()
      if (g && s.fadeOnPause) {
        g.setVolume(0)
        g.fadeTo(stateRef.current.volume, 0.35)
      }
    } else {
      if (g && s.fadeOnPause) {
        g.fadeTo(0.0001, 0.35).then(() => {
          audio.pause()
          g.setVolume(stateRef.current.volume)
        })
      } else {
        audio.pause()
      }
    }
  }, [current])
  const togglePlayRef = useRef(togglePlay)
  togglePlayRef.current = togglePlay

  const next = useCallback(() => goNext(false), [])
  const prev = useCallback(() => {
    const audio = audioRef.current
    if (audio && audio.currentTime > 3) {
      audio.currentTime = 0
      return
    }
    setIndex((i) => (i > 0 ? i - 1 : i))
  }, [])
  const prevRef = useRef(prev)
  prevRef.current = prev

  const seek = useCallback((sec: number) => {
    const audio = audioRef.current
    if (audio) {
      audio.currentTime = sec
      setProgress(sec)
    }
  }, [])

  const setVolume = useCallback((v: number) => {
    setVolumeState(v)
    graphRef.current?.setVolume(v)
    if (audioRef.current && !graphRef.current) audioRef.current.volume = v
    localStorage.setItem('highmusic.volume', String(v))
  }, [])

  const toggleShuffle = useCallback(() => setShuffle((s) => !s), [])
  const cycleRepeat = useCallback(
    () => setRepeat((r) => (r === 'off' ? 'all' : r === 'all' ? 'one' : 'off')),
    []
  )
  const jumpTo = useCallback((i: number) => setIndex(i), [])

  const logout = useCallback(() => {
    audioRef.current?.pause()
    setQueue([])
    setIndex(0)
    setCreds(null)
  }, [setCreds])

  const value: AppState = {
    creds,
    setCreds,
    logout,
    view,
    param,
    navigate,
    queue,
    index,
    current,
    isPlaying,
    progress,
    duration,
    volume,
    shuffle,
    repeat,
    playQueue,
    playNow,
    addToQueue,
    removeFromQueue,
    reorderQueue,
    togglePlay,
    next,
    prev,
    seek,
    setVolume,
    toggleShuffle,
    cycleRepeat,
    jumpTo,
    settings,
    fullscreen,
    setFullscreen,
    mini
  }

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}
