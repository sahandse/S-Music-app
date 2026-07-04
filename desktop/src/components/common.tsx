import React from 'react'
import { useApp } from '../store'
import { coverUrl, streamUrl, Song, fmtDuration, star, unstar } from '../api'
import { bridge, isDesktop } from '../desktop'
import { getSettings, setSettings } from '../settings'
import { t } from '../i18n'

// Cover image that falls back to a music-note glyph when there's no art.
// `directUrl` (for user-added songs) takes priority over the Subsonic id.
export function Cover({
  id,
  directUrl,
  size = 300,
  className = ''
}: {
  id?: string
  directUrl?: string
  size?: number
  className?: string
}) {
  const { creds } = useApp()
  const url = directUrl || coverUrl(creds, id, size)
  const [failed, setFailed] = React.useState(false)
  React.useEffect(() => setFailed(false), [url])
  if (!url || failed) {
    return <div className={`cover-fallback ${className}`}>♪</div>
  }
  return <img className={className} src={url} onError={() => setFailed(true)} alt="" />
}

// Reusable track list with play-on-click and star toggle
export function TrackList({
  songs,
  showAlbum = true
}: {
  songs: Song[]
  showAlbum?: boolean
}) {
  const { playQueue, current, isPlaying, creds, setFullscreen } = useApp()
  const [starred, setStarred] = React.useState<Record<string, boolean>>({})
  const [dl, setDl] = React.useState<Record<string, 'busy' | 'done' | 'err'>>({})

  // Clicking a track plays it and opens the full-screen player with lyrics.
  const openAndPlay = (i: number) => {
    playQueue(songs, i)
    setFullscreen(true)
  }

  const toggleStar = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation()
    if (!creds) return
    const on = !starred[id]
    setStarred((s) => ({ ...s, [id]: on }))
    try {
      on ? await star(creds, id) : await unstar(creds, id)
    } catch {
      setStarred((s) => ({ ...s, [id]: !on }))
    }
  }

  const download = async (e: React.MouseEvent, s: Song) => {
    e.stopPropagation()
    const b = bridge()
    if (!b || !creds) return
    let folder = getSettings().downloadFolder
    if (!folder) {
      folder = (await b.pickFolder()) || ''
      if (!folder) return
      setSettings({ downloadFolder: folder })
    }
    const url = s.directUrl || streamUrl(creds, s.id)
    const name = `${(s.artist || '').trim()} - ${s.title}`.trim().replace(/^ - /, '') + '.mp3'
    setDl((d) => ({ ...d, [s.id]: 'busy' }))
    try {
      await b.downloadFile(url, folder, name)
      setDl((d) => ({ ...d, [s.id]: 'done' }))
    } catch {
      setDl((d) => ({ ...d, [s.id]: 'err' }))
    }
  }

  if (!songs.length) return <div className="empty">{t('common.noSongs')}</div>

  return (
    <div>
      {songs.map((s, i) => {
        const active = current?.id === s.id
        return (
          <div
            key={s.id + i}
            className={`track-row ${active ? 'playing' : ''}`}
            onDoubleClick={() => openAndPlay(i)}
          >
            <div className="t-index" onClick={() => openAndPlay(i)}>
              {active && isPlaying ? '▶' : i + 1}
            </div>
            <div className="t-title" onClick={() => openAndPlay(i)}>
              {s.title}
              <div className="card-sub" style={{ marginTop: 2 }}>
                {s.artist}
              </div>
            </div>
            <div className="t-album">{showAlbum ? s.album : ''}</div>
            <div className="t-dur">{fmtDuration(s.duration)}</div>
            {isDesktop() && (
              <div
                className="t-star"
                onClick={(e) => download(e, s)}
                title={t('common.download')}
              >
                {dl[s.id] === 'busy' ? '…' : dl[s.id] === 'done' ? '✓' : dl[s.id] === 'err' ? '⚠' : '⬇'}
              </div>
            )}
            <div
              className={`t-star ${starred[s.id] ? 'on' : ''}`}
              onClick={(e) => toggleStar(e, s.id)}
              title={t('common.like')}
            >
              {starred[s.id] ? '♥' : '♡'}
            </div>
          </div>
        )
      })}
    </div>
  )
}
