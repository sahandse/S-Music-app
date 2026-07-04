import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbum, star, unstar, Song, fmtDuration } from '../api'
import { Cover, TrackList } from '../components/common'
import { t } from '../i18n'

export function AlbumDetail({ id }: { id: string }) {
  const { creds, playQueue, addToQueue, navigate } = useApp()
  const [album, setAlbum] = useState<Album | null>(null)
  const [songs, setSongs] = useState<Song[]>([])
  const [liked, setLiked] = useState(false)
  const [loading, setLoading] = useState(true)

  const toggleLike = async () => {
    if (!creds) return
    const on = !liked
    setLiked(on)
    try {
      on ? await star(creds, id) : await unstar(creds, id)
    } catch {
      setLiked(!on)
    }
  }

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getAlbum(creds, id)
      .then(({ album, songs }) => {
        setAlbum(album)
        setSongs(songs)
        setLiked(!!album.starred)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds, id])

  if (loading) return <div className="empty">{t('common.loading')}</div>
  if (!album) return <div className="empty">{t('albums.notFound')}</div>

  const total = songs.reduce((a, s) => a + (s.duration || 0), 0)

  return (
    <div>
      <div className="detail-head">
        <Cover id={album.coverArt} size={400} />
        <div className="detail-meta">
          <div className="kind">{t('album.kind')}</div>
          <h1>{album.name}</h1>
          <div
            className="sub"
            style={{ cursor: album.artistId ? 'pointer' : 'default' }}
            onClick={() => album.artistId && navigate('artist', album.artistId)}
          >
            {album.artist}
            {album.year ? ` · ${album.year}` : ''} · {songs.length} {t('common.songs')} ·{' '}
            {fmtDuration(total)}
          </div>
          <div className="row-actions">
            <button className="btn" onClick={() => playQueue(songs, 0)}>
              ▶ {t('common.play')}
            </button>
            <button className="btn ghost" onClick={() => addToQueue(songs)}>
              + {t('album.addQueue')}
            </button>
            <button className={`btn ghost follow-btn ${liked ? 'on' : ''}`} onClick={toggleLike}>
              {liked ? '♥ ' + t('album.liked') : '♡ ' + t('album.like')}
            </button>
          </div>
        </div>
      </div>
      <TrackList songs={songs} showAlbum={false} />
    </div>
  )
}
