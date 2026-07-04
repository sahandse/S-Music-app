import React, { useEffect, useRef, useState } from 'react'
import { useApp } from '../store'
import { coverUrl, fmtDuration } from '../api'
import { fetchLyrics, LyricLine } from '../lyrics'

export function NowPlaying() {
  const {
    current,
    creds,
    isPlaying,
    togglePlay,
    next,
    prev,
    progress,
    duration,
    seek,
    shuffle,
    toggleShuffle,
    repeat,
    cycleRepeat,
    setFullscreen
  } = useApp()

  const [lyrics, setLyrics] = useState<LyricLine[]>([])
  const [bg, setBg] = useState('#1a1a1f')
  const lyricsRef = useRef<HTMLDivElement | null>(null)

  const cover = current
    ? current.directUrl
      ? current.coverImageUrl || ''
      : coverUrl(creds, current.coverArt, 600)
    : ''

  // Fetch lyrics on track change
  useEffect(() => {
    setLyrics([])
    if (!current) return
    fetchLyrics(current.title, current.artist || '').then(setLyrics)
  }, [current?.id])

  // Derive a dominant color from the cover for the background
  useEffect(() => {
    if (!cover) {
      setBg('#1a1a1f')
      return
    }
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      try {
        const c = document.createElement('canvas')
        c.width = c.height = 16
        const ctx = c.getContext('2d')!
        ctx.drawImage(img, 0, 0, 16, 16)
        const d = ctx.getImageData(0, 0, 16, 16).data
        let r = 0, g = 0, b = 0, n = 0
        for (let i = 0; i < d.length; i += 4) {
          r += d[i]; g += d[i + 1]; b += d[i + 2]; n++
        }
        r = Math.round(r / n); g = Math.round(g / n); b = Math.round(b / n)
        // darken for readability
        setBg(`rgb(${Math.round(r * 0.5)}, ${Math.round(g * 0.5)}, ${Math.round(b * 0.5)})`)
      } catch {
        setBg('#1a1a1f')
      }
    }
    img.onerror = () => setBg('#1a1a1f')
    img.src = cover
  }, [cover])

  // Current synced lyric line
  const active =
    lyrics.length && lyrics[0].time >= 0
      ? lyrics.reduce((acc, l, i) => (l.time <= progress ? i : acc), -1)
      : -1

  useEffect(() => {
    if (active >= 0 && lyricsRef.current) {
      const el = lyricsRef.current.querySelector(`[data-i="${active}"]`) as HTMLElement
      if (el) el.scrollIntoView({ block: 'center', behavior: 'smooth' })
    }
  }, [active])

  if (!current) return null

  return (
    <div className="nowplaying" style={{ background: `linear-gradient(160deg, ${bg}, #0b0b0e 85%)` }}>
      <button className="np-close" onClick={() => setFullscreen(false)} title="بستن">
        ⌄
      </button>
      <div className="np-body">
        <div className="np-left">
          {cover ? (
            <img className="np-cover" src={cover} alt="" />
          ) : (
            <div className="np-cover cover-fallback">♪</div>
          )}
          <div className="np-meta">
            <div className="np-title">{current.title}</div>
            <div className="np-artist">{current.artist}</div>
          </div>
          <div className="np-seek">
            <span className="time">{fmtDuration(progress)}</span>
            <input
              type="range"
              min={0}
              max={duration || 0}
              step={1}
              value={progress}
              onChange={(e) => seek(parseFloat(e.target.value))}
            />
            <span className="time">{fmtDuration(duration)}</span>
          </div>
          <div className="np-controls">
            <button className={shuffle ? 'active' : ''} onClick={toggleShuffle}>🔀</button>
            <button onClick={prev}>⏮</button>
            <button className="play-btn" onClick={togglePlay}>{isPlaying ? '⏸' : '▶'}</button>
            <button onClick={next}>⏭</button>
            <button className={repeat !== 'off' ? 'active' : ''} onClick={cycleRepeat}>
              {repeat === 'one' ? '🔂' : '🔁'}
            </button>
          </div>
        </div>
        <div className="np-lyrics" ref={lyricsRef}>
          {lyrics.length === 0 && <div className="empty">متن آهنگ یافت نشد</div>}
          {lyrics.map((l, i) => (
            <div
              key={i}
              data-i={i}
              className={`lyric-line ${i === active ? 'active' : ''} ${l.time < 0 ? 'plain' : ''}`}
              onClick={() => l.time >= 0 && seek(l.time)}
            >
              {l.text || '♪'}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
