import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbum, Song, fmtDuration } from '../api'
import { Cover, TrackList } from '../components/common'

export function AlbumDetail({ id }: { id: string }) {
  const { creds, playQueue, addToQueue, navigate } = useApp()
  const [album, setAlbum] = useState<Album | null>(null)
  const [songs, setSongs] = useState<Song[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getAlbum(creds, id)
      .then(({ album, songs }) => {
        setAlbum(album)
        setSongs(songs)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds, id])

  if (loading) return <div className="empty">در حال بارگذاری...</div>
  if (!album) return <div className="empty">آلبوم پیدا نشد</div>

  const total = songs.reduce((a, s) => a + (s.duration || 0), 0)

  return (
    <div>
      <div className="detail-head">
        <Cover id={album.coverArt} size={400} />
        <div className="detail-meta">
          <div className="kind">آلبوم</div>
          <h1>{album.name}</h1>
          <div
            className="sub"
            style={{ cursor: album.artistId ? 'pointer' : 'default' }}
            onClick={() => album.artistId && navigate('artist', album.artistId)}
          >
            {album.artist}
            {album.year ? ` · ${album.year}` : ''} · {songs.length} آهنگ ·{' '}
            {fmtDuration(total)}
          </div>
          <div className="row-actions">
            <button className="btn" onClick={() => playQueue(songs, 0)}>
              ▶ پخش
            </button>
            <button className="btn ghost" onClick={() => addToQueue(songs)}>
              + به صف
            </button>
          </div>
        </div>
      </div>
      <TrackList songs={songs} showAlbum={false} />
    </div>
  )
}
