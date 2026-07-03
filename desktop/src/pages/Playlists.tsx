import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { getPlaylist, getPlaylists, Playlist, Song, fmtDuration } from '../api'
import { Cover, TrackList } from '../components/common'

export function Playlists() {
  const { creds, navigate } = useApp()
  const [playlists, setPlaylists] = useState<Playlist[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getPlaylists(creds)
      .then(setPlaylists)
      .catch(() => setPlaylists([]))
      .finally(() => setLoading(false))
  }, [creds])

  return (
    <div>
      <div className="page-title">پلی‌لیست‌ها</div>
      {loading ? (
        <div className="empty">در حال بارگذاری...</div>
      ) : playlists.length === 0 ? (
        <div className="empty">پلی‌لیستی وجود ندارد</div>
      ) : (
        <div className="grid">
          {playlists.map((p) => (
            <div key={p.id} className="card" onClick={() => navigate('playlist', p.id)}>
              <Cover id={p.coverArt} />
              <div className="card-title">{p.name}</div>
              <div className="card-sub">{p.songCount || 0} آهنگ</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export function PlaylistDetail({ id }: { id: string }) {
  const { creds, playQueue } = useApp()
  const [playlist, setPlaylist] = useState<Playlist | null>(null)
  const [songs, setSongs] = useState<Song[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getPlaylist(creds, id)
      .then(({ playlist, songs }) => {
        setPlaylist(playlist)
        setSongs(songs)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds, id])

  if (loading) return <div className="empty">در حال بارگذاری...</div>
  if (!playlist) return <div className="empty">پلی‌لیست پیدا نشد</div>

  const total = songs.reduce((a, s) => a + (s.duration || 0), 0)

  return (
    <div>
      <div className="detail-head">
        <Cover id={playlist.coverArt} size={400} />
        <div className="detail-meta">
          <div className="kind">پلی‌لیست</div>
          <h1>{playlist.name}</h1>
          <div className="sub">
            {songs.length} آهنگ · {fmtDuration(total)}
          </div>
          <button className="btn" onClick={() => playQueue(songs, 0)}>
            ▶ پخش
          </button>
        </div>
      </div>
      <TrackList songs={songs} />
    </div>
  )
}
