import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react'
import { Creds, Song, streamUrl } from './api'

const CREDS_KEY = 'highmusic.creds'

type RepeatMode = 'off' | 'all' | 'one'

interface AppState {
  creds: Creds | null
  setCreds: (c: Creds | null) => void
  logout: () => void

  // navigation
  view: string
  param: string | null
  navigate: (view: string, param?: string | null) => void

  // playback
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
  togglePlay: () => void
  next: () => void
  prev: () => void
  seek: (sec: number) => void
  setVolume: (v: number) => void
  toggleShuffle: () => void
  cycleRepeat: () => void
  jumpTo: (i: number) => void
}

const Ctx = createContext<AppState | null>(null)

export function useApp(): AppState {
  const v = useContext(Ctx)
  if (!v) throw new Error('useApp outside provider')
  return v
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

  const audioRef = useRef<HTMLAudioElement | null>(null)
  if (!audioRef.current && typeof Audio !== 'undefined') {
    audioRef.current = new Audio()
  }

  const current = queue[index] ?? null

  const setCreds = useCallback((c: Creds | null) => {
    setCredsState(c)
    if (c) localStorage.setItem(CREDS_KEY, JSON.stringify(c))
    else localStorage.removeItem(CREDS_KEY)
  }, [])

  const navigate = useCallback((v: string, p: string | null = null) => {
    setView(v)
    setParam(p)
  }, [])

  // Load & play whatever song is at `index`
  useEffect(() => {
    const audio = audioRef.current
    if (!audio || !current) return
    // Custom songs carry a direct URL; server songs stream via Subsonic
    const src = current.directUrl
      ? current.directUrl
      : creds
        ? streamUrl(creds, current.id)
        : ''
    if (!src) return
    audio.src = src
    audio.volume = volume
    audio.play().then(() => setIsPlaying(true)).catch(() => setIsPlaying(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index, queue, creds])

  // Audio element event wiring
  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return
    const onTime = () => setProgress(audio.currentTime)
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
  }, [queue, index, repeat, shuffle])

  const handleEnded = () => {
    if (repeat === 'one') {
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
    setIndex((i) => {
      if (shuffle && queue.length > 1) {
        let r = i
        while (r === i) r = Math.floor(Math.random() * queue.length)
        return r
      }
      if (i < queue.length - 1) return i + 1
      if (repeat === 'all') return 0
      return auto ? i : i // stop at end
    })
  }

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

  const togglePlay = useCallback(() => {
    const audio = audioRef.current
    if (!audio || !current) return
    if (audio.paused) audio.play()
    else audio.pause()
  }, [current])

  const next = useCallback(() => goNext(false), [queue, shuffle, repeat])
  const prev = useCallback(() => {
    const audio = audioRef.current
    if (audio && audio.currentTime > 3) {
      audio.currentTime = 0
      return
    }
    setIndex((i) => (i > 0 ? i - 1 : i))
  }, [])

  const seek = useCallback((sec: number) => {
    const audio = audioRef.current
    if (audio) {
      audio.currentTime = sec
      setProgress(sec)
    }
  }, [])

  const setVolume = useCallback((v: number) => {
    setVolumeState(v)
    if (audioRef.current) audioRef.current.volume = v
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
    togglePlay,
    next,
    prev,
    seek,
    setVolume,
    toggleShuffle,
    cycleRepeat,
    jumpTo
  }

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}
