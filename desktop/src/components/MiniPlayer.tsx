import React from 'react'
import { useApp } from '../store'
import { Cover } from './common'
import { bridge } from '../desktop'
import { t } from '../i18n'

// Compact always-on-top player shown when the window is in mini mode.
export function MiniPlayer() {
  const { current, isPlaying, togglePlay, next, prev, progress, duration } = useApp()
  const pct = duration > 0 ? Math.min(100, Math.max(0, (progress / duration) * 100)) : 0

  return (
    <div className="mini-player">
      <div className="mini-main">
        {current ? (
          <Cover id={current.coverArt} directUrl={current.coverImageUrl} size={64} className="mini-cover" />
        ) : (
          <div className="cover-fallback mini-cover">♪</div>
        )}
        <div className="mini-info">
          <div className="p-title">{current ? current.title : t('player.nothing2')}</div>
          <div className="card-sub">{current?.artist}</div>
        </div>
        <div className="mini-controls">
          <button aria-label={t('player.prev')} onClick={prev}>⏮</button>
          <button aria-label={t('player.playPause')} className="play-btn" onClick={togglePlay}>
            {isPlaying ? '⏸' : '▶'}
          </button>
          <button aria-label={t('player.next')} onClick={next}>⏭</button>
          <button title={t('player.restore')} onClick={() => bridge()?.setMini(false)}>
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
