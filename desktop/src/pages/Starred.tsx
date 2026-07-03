import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { getStarred, Song, Album } from '../api'
import { Cover, TrackList } from '../components/common'

export function Starred() {
  const { creds, navigate, playQueue } = useApp()
  const [songs, setSongs] = useState<Song[]>([])
  const [albums, setAlbums] = useState<Album[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getStarred(creds)
      .then(({ songs, albums }) => {
        setSongs(songs)
        setAlbums(albums)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds])

  if (loading) return <div className="empty">در حال بارگذاری...</div>

  return (
    <div>
      <div className="page-title">پسندیده‌ها</div>
      {albums.length > 0 && (
        <>
          <div className="section-title">آلبوم‌های پسندیده</div>
          <div className="grid">
            {albums.map((a) => (
              <div key={a.id} className="card" onClick={() => navigate('album', a.id)}>
                <Cover id={a.coverArt} />
                <div className="card-title">{a.name}</div>
                <div className="card-sub">{a.artist}</div>
              </div>
            ))}
          </div>
        </>
      )}
      <div className="section-title">
        آهنگ‌های پسندیده{' '}
        {songs.length > 0 && (
          <button className="chip active" style={{ marginRight: 12 }} onClick={() => playQueue(songs, 0)}>
            پخش همه
          </button>
        )}
      </div>
      {songs.length ? <TrackList songs={songs} /> : <div className="empty">آهنگ پسندیده‌ای نیست</div>}
    </div>
  )
}
