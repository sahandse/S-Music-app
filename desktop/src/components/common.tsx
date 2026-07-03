import React from 'react'
import { useApp } from '../store'
import { coverUrl, Song, fmtDuration, star, unstar } from '../api'

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
  const { playQueue, current, isPlaying, creds } = useApp()
  const [starred, setStarred] = React.useState<Record<string, boolean>>({})

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

  if (!songs.length) return <div className="empty">آهنگی نیست</div>

  return (
    <div>
      {songs.map((s, i) => {
        const active = current?.id === s.id
        return (
          <div
            key={s.id + i}
            className={`track-row ${active ? 'playing' : ''}`}
            onDoubleClick={() => playQueue(songs, i)}
          >
            <div className="t-index" onClick={() => playQueue(songs, i)}>
              {active && isPlaying ? '▶' : i + 1}
            </div>
            <div className="t-title" onClick={() => playQueue(songs, i)}>
              {s.title}
              <div className="card-sub" style={{ marginTop: 2 }}>
                {s.artist}
              </div>
            </div>
            <div className="t-album">{showAlbum ? s.album : ''}</div>
            <div className="t-dur">{fmtDuration(s.duration)}</div>
            <div
              className={`t-star ${starred[s.id] ? 'on' : ''}`}
              onClick={(e) => toggleStar(e, s.id)}
              title="پسندیدن"
            >
              {starred[s.id] ? '♥' : '♡'}
            </div>
          </div>
        )
      })}
    </div>
  )
}
