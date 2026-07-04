import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, Artist, getArtist, getTopSongs, star, unstar, Song } from '../api'
import { Cover, TrackList } from '../components/common'
import { t } from '../i18n'
import { fetchArtistBio } from '../wiki'

export function ArtistDetail({ id }: { id: string }) {
  const { creds, navigate, playQueue, settings } = useApp()
  void settings.lang
  const [artist, setArtist] = useState<Artist | null>(null)
  const [albums, setAlbums] = useState<Album[]>([])
  const [top, setTop] = useState<Song[]>([])
  const [bio, setBio] = useState('')
  const [bioOpen, setBioOpen] = useState(false)
  const [following, setFollowing] = useState(false)
  const [loading, setLoading] = useState(true)

  const toggleFollow = async () => {
    if (!creds) return
    const on = !following
    setFollowing(on)
    try {
      on ? await star(creds, id) : await unstar(creds, id)
    } catch {
      setFollowing(!on)
    }
  }

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    setBio('')
    setBioOpen(false)
    getArtist(creds, id)
      .then(async ({ artist, albums }) => {
        setArtist(artist)
        setAlbums(albums)
        setFollowing(!!artist.starred)
        fetchArtistBio(artist.name).then(setBio)
        try {
          setTop(await getTopSongs(creds, artist.name, 10))
        } catch {
          setTop([])
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds, id, settings.lang])

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
          <div className="row-actions">
            {top.length > 0 && (
              <button className="btn" onClick={() => playQueue(top, 0)}>
                ▶ {t('artist.topPlay')}
              </button>
            )}
            <button className={`btn ghost follow-btn ${following ? 'on' : ''}`} onClick={toggleFollow}>
              {following ? '✓ ' + t('artist.following') : '+ ' + t('artist.follow')}
            </button>
          </div>
        </div>
      </div>

      {bio && (
        <div className={`artist-bio ${bioOpen ? 'open' : ''}`}>
          <div className="section-title">{t('artist.bio')}</div>
          <p>{bio}</p>
          {bio.length > 280 && (
            <button className="bio-toggle" onClick={() => setBioOpen((v) => !v)}>
              {bioOpen ? t('artist.less') : t('artist.more')}
            </button>
          )}
        </div>
      )}

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
