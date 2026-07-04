import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { getStarred, Song, Album, Artist } from '../api'
import { Cover, TrackList } from '../components/common'
import { t } from '../i18n'

export function Starred() {
  const { creds, navigate, playQueue, settings } = useApp()
  void settings.lang
  const [songs, setSongs] = useState<Song[]>([])
  const [albums, setAlbums] = useState<Album[]>([])
  const [artists, setArtists] = useState<Artist[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getStarred(creds)
      .then(({ songs, albums, artists }) => {
        setSongs(songs)
        setAlbums(albums)
        setArtists(artists)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds])

  if (loading) return <div className="empty">{t('common.loading')}</div>

  return (
    <div>
      <div className="page-title">{t('nav.starred')}</div>
      {artists.length > 0 && (
        <>
          <div className="section-title">{t('starred.followedArtists')}</div>
          <div className="grid">
            {artists.map((a) => (
              <div key={a.id} className="card round" onClick={() => navigate('artist', a.id)}>
                <Cover id={a.coverArt} />
                <div className="card-title">{a.name}</div>
              </div>
            ))}
          </div>
        </>
      )}
      {albums.length > 0 && (
        <>
          <div className="section-title">{t('starred.likedAlbums')}</div>
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
        {t('nav.starred')}{' '}
        {songs.length > 0 && (
          <button className="chip active" style={{ marginInlineStart: 12 }} onClick={() => playQueue(songs, 0)}>
            {t('common.playAll')}
          </button>
        )}
      </div>
      {songs.length ? <TrackList songs={songs} /> : <div className="empty">{t('starred.noLiked')}</div>}
    </div>
  )
}
