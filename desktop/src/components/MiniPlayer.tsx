import React from 'react'
import { useApp } from '../store'
import { Cover } from './common'
import { bridge } from '../desktop'

// Compact always-on-top player shown when the window is in mini mode.
export function MiniPlayer() {
  const { current, isPlaying, togglePlay, next, prev, progress, duration } = useApp()
  const pct = duration ? (progress / duration) * 100 : 0

  return (
    <div className="mini-player">
      <div className="mini-main">
        {current ? (
          <Cover id={current.coverArt} directUrl={current.coverImageUrl} size={64} className="mini-cover" />
        ) : (
          <div className="cover-fallback mini-cover">♪</div>
        )}
        <div className="mini-info">
          <div className="p-title">{current ? current.title : 'چیزی پخش نمی‌شود'}</div>
          <div className="card-sub">{current?.artist}</div>
        </div>
        <div className="mini-controls">
          <button onClick={prev}>⏮</button>
          <button className="play-btn" onClick={togglePlay}>
            {isPlaying ? '⏸' : '▶'}
          </button>
          <button onClick={next}>⏭</button>
          <button title="بازگشت به حالت عادی" onClick={() => bridge()?.setMini(false)}>
            ⤢
          </button>
        </div>
      </div>
      <div className="mini-bar">
        <div className="mini-bar-fill" style={{ width: pct + '%' }} />
      </div>
    </div>
  )
}
