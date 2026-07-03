import React from 'react'
import { useApp } from '../store'
import { fmtDuration } from '../api'
import { Cover } from './common'

export function PlayerBar({ onToggleQueue }: { onToggleQueue: () => void }) {
  const {
    current,
    isPlaying,
    togglePlay,
    next,
    prev,
    progress,
    duration,
    seek,
    volume,
    setVolume,
    shuffle,
    toggleShuffle,
    repeat,
    cycleRepeat,
    navigate
  } = useApp()

  return (
    <div className="player">
      <div className="player-track">
        {current ? (
          <>
            <Cover id={current.coverArt} size={120} />
            <div className="info">
              <div className="p-title">{current.title}</div>
              <div
                className="p-artist"
                style={{ cursor: current.artistId ? 'pointer' : 'default' }}
                onClick={() => current.artistId && navigate('artist', current.artistId)}
              >
                {current.artist}
              </div>
            </div>
          </>
        ) : (
          <div className="p-artist">چیزی در حال پخش نیست</div>
        )}
      </div>

      <div className="player-center">
        <div className="controls">
          <button className={shuffle ? 'active' : ''} onClick={toggleShuffle} title="تصادفی">
            🔀
          </button>
          <button onClick={prev} title="قبلی">
            ⏮
          </button>
          <button className="play-btn" onClick={togglePlay} title="پخش/توقف">
            {isPlaying ? '⏸' : '▶'}
          </button>
          <button onClick={next} title="بعدی">
            ⏭
          </button>
          <button
            className={repeat !== 'off' ? 'active' : ''}
            onClick={cycleRepeat}
            title="تکرار"
          >
            {repeat === 'one' ? '🔂' : '🔁'}
          </button>
        </div>
        <div className="seek">
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
      </div>

      <div className="player-right">
        <div className="vol">
          <span>🔊</span>
          <input
            type="range"
            min={0}
            max={1}
            step={0.01}
            value={volume}
            onChange={(e) => setVolume(parseFloat(e.target.value))}
          />
        </div>
        <button onClick={onToggleQueue} title="صف پخش">
          ☰
        </button>
      </div>
    </div>
  )
}
