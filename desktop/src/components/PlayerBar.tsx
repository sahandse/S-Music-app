import React, { useState } from 'react'
import { useApp } from '../store'
import { fmtDuration } from '../api'
import { Cover } from './common'
import { bridge, isDesktop } from '../desktop'
import { t } from '../i18n'

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
    navigate,
    setFullscreen,
    settings,
    sleep,
    setSleep,
    sleepRemaining
  } = useApp()
  void settings.lang
  const [sleepMenu, setSleepMenu] = useState(false)

  const sleepActive = sleep !== 'off'
  const sleepLabel =
    sleep === 'track'
      ? t('sleep.track')
      : typeof sleep === 'number'
        ? fmtDuration(sleepRemaining)
        : ''
  const SLEEP_OPTS: Array<'off' | 'track' | number> = ['off', 15, 30, 45, 60, 'track']

  return (
    <div className="player">
      <div className="player-track">
        {current ? (
          <>
            <div
              className="player-cover-btn"
              onClick={() => setFullscreen(true)}
              title={t('player.fullscreen')}
            >
              <Cover id={current.coverArt} directUrl={current.coverImageUrl} size={120} />
              <span className="cover-expand">⤢</span>
            </div>
            <div className="info">
              <div className="p-title" onClick={() => setFullscreen(true)}>
                {current.title}
              </div>
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
          <div className="p-artist">{t('player.nothing')}</div>
        )}
      </div>

      <div className="player-center">
        <div className="controls">
          <button className={(shuffle || smartShuffle) ? 'active' : ''} onClick={toggleShuffle} title={t('player.shuffle')}>
            {smartShuffle ? '✨' : '🔀'}
          </button>
          <button onClick={prev} title={t('player.prev')}>
            ⏮
          </button>
          <button className="play-btn" onClick={togglePlay} title={t('player.playPause')}>
            {isPlaying ? '⏸' : '▶'}
          </button>
          <button onClick={next} title={t('player.next')}>
            ⏭
          </button>
          <button
            className={repeat !== 'off' ? 'active' : ''}
            onClick={cycleRepeat}
            title={t('player.repeat')}
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
        <div className="sleep-wrap">
          <button
            className={sleepActive ? 'active' : ''}
            onClick={() => setSleepMenu((v) => !v)}
            title={t('sleep.title')}
          >
            ⏱{sleepActive && <span className="sleep-badge">{sleepLabel}</span>}
          </button>
          {sleepMenu && (
            <div className="sleep-menu" onMouseLeave={() => setSleepMenu(false)}>
              {SLEEP_OPTS.map((o) => (
                <div
                  key={String(o)}
                  className={`sleep-item ${sleep === o ? 'on' : ''}`}
                  onClick={() => {
                    setSleep(o)
                    setSleepMenu(false)
                  }}
                >
                  {o === 'off' ? t('sleep.off') : o === 'track' ? t('sleep.track') : t('sleep.min', { n: o })}
                </div>
              ))}
            </div>
          )}
        </div>
        <button onClick={() => setFullscreen(true)} title={t('player.fullscreenLyrics')}>
          ⤢
        </button>
        {isDesktop() && (
          <button onClick={() => bridge()?.setMini(true)} title={t('player.mini')}>
            ▭
          </button>
        )}
        <button onClick={onToggleQueue} title={t('player.queue')}>
          ☰
        </button>
      </div>
    </div>
  )
}
