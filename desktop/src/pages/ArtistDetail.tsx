import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, Artist, getArtist, getTopSongs, Song } from '../api'
import { Cover, TrackList } from '../components/common'
import { t } from '../i18n'

export function ArtistDetail({ id }: { id: string }) {
  const { creds, navigate, playQueue } = useApp()
  const [artist, setArtist] = useState<Artist | null>(null)
  const [albums, setAlbums] = useState<Album[]>([])
  const [top, setTop] = useState<Song[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getArtist(creds, id)
      .then(async ({ artist, albums }) => {
        setArtist(artist)
        setAlbums(albums)
        try {
          setTop(await getTopSongs(creds, artist.name, 10))
        } catch {
          setTop([])
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds, id])

  if (loading) return <div className="empty">{t('common.loading')}</div>
  if (!artist) return <div className="empty">{t('artists.notFound')}</div>

  return (
    <div>
      <div className="detail-head round">
        <Cover id={artist.coverArt} size={400} />
        <div className="detail-meta">
          <div className="kind">{t('artist.kind')}</div>
          <h1>{artist.name}</h1>
          <div className="sub">
            {albums.length} {t('album.albums')}
          </div>
          {top.length > 0 && (
            <button className="btn" onClick={() => playQueue(top, 0)}>
              ▶ {t('artist.topPlay')}
            </button>
          )}
        </div>
      </div>

      {top.length > 0 && (
        <>
          <div className="section-title">{t('artist.topSongs')}</div>
          <TrackList songs={top} />
        </>
      )}

      <div className="section-title">{t('nav.albums')}</div>
      <div className="grid">
        {albums.map((a) => (
          <div key={a.id} className="card" onClick={() => navigate('album', a.id)}>
            <Cover id={a.coverArt} />
            <div className="card-title">{a.name}</div>
            <div className="card-sub">{a.year || ''}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
