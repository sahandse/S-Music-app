import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbumList, getGenres, getRandomSongs, getSongsByGenre, Song } from '../api'
import { Cover, TrackList } from '../components/common'
import { catalogToSong, fetchCatalog } from '../catalog'
import { t } from '../i18n'

export function Home() {
  const { creds, navigate, playQueue, settings } = useApp()
  void settings.lang
  const [recent, setRecent] = useState<Album[]>([])
  const [frequent, setFrequent] = useState<Album[]>([])
  const [random, setRandom] = useState<Song[]>([])
  const [newSongs, setNewSongs] = useState<Song[]>([])
  const [genres, setGenres] = useState<string[]>([])
  const [genre, setGenre] = useState<string>('')
  const [genreSongs, setGenreSongs] = useState<Song[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    setLoadError(false)
    Promise.all([
      getAlbumList(creds, 'newest', 18),
      getAlbumList(creds, 'frequent', 18),
      getRandomSongs(creds, 20)
    ])
      .then(([n, f, r]) => {
        setRecent(n)
        setFrequent(f)
        setRandom(r)
      })
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
    getGenres(creds)
      .then((g) => {
        setGenres(g)
        if (g.length) setGenre(g[0])
      })
      .catch(() => setGenres([]))
    // Latest shared-catalog songs ("Newest Iranian"), refreshed periodically so
    // songs newly posted to the channel appear without a manual reload.
    const loadCatalog = () =>
      fetchCatalog()
        .then((list) => setNewSongs(list.slice(0, 12).map(catalogToSong)))
        .catch(() => {})
    loadCatalog()
    const id = setInterval(loadCatalog, 60000)
    return () => clearInterval(id)
  }, [creds, retry])

  useEffect(() => {
    if (!creds || !genre) return
    getSongsByGenre(creds, genre, 20)
      .then(setGenreSongs)
      .catch(() => setGenreSongs([]))
  }, [creds, genre])

  if (loading) return <div className="home-loading" role="status" aria-label={t('common.loading')}><div className="skeleton-heading" /> <div className="grid">{Array.from({ length: 8 }, (_, i) => <div className="skeleton-card" key={i}><div className="skeleton-cover"/><div className="skeleton-line"/><div className="skeleton-line short"/></div>)}</div><span className="sr-only">{t('common.loading')}</span></div>

  return (
    <div>
      <div className="page-title">{t('nav.home')}</div>
      {loadError && <div className="home-error" role="alert"><span>Could not load server music.</span><button type="button" className="btn-sm" onClick={() => setRetry(v => v + 1)}>Retry</button></div>

      {newSongs.length > 0 && (
        <>
          <div className="section-title">
            {t('home.newestIranian')}{' '}
            <button
              className="chip active"
              style={{ marginInlineStart: 12 }}
              onClick={() => playQueue(newSongs, 0)}
            >
              {t('common.playAll')}
            </button>
          </div>
          <TrackList songs={newSongs} showAlbum={false} />
        </>
      )}

      <AlbumRow title={t('home.newest')} albums={recent} onOpen={(id) => navigate('album', id)} />
      <AlbumRow title={t('home.frequent')} albums={frequent} onOpen={(id) => navigate('album', id)} />

      <div className="section-title">
        {t('home.random')}{' '}
        <button
          className="chip active"
          style={{ marginInlineStart: 12 }}
          onClick={() => playQueue(random, 0)}
        >
          {t('common.playAll')}
        </button>
      </div>
      <TrackList songs={random} />

      {genres.length > 0 && (
        <>
          <div className="section-title">{t('home.genres')}</div>
          <div style={{ marginBottom: 14 }}>
            {genres.map((g) => (
              <button type="button"
                key={g}
                className={`chip ${genre === g ? 'active' : ''}`}
                aria-pressed={genre === g}
                onClick={() => setGenre(g)}
              >
                {g}
              </button>
            ))}
          </div>
          <TrackList songs={genreSongs} />
        </>
      )}
    </div>
  )
}

export function AlbumRow({
  title,
  albums,
  onOpen
}: {
  title: string
  albums: Album[]
  onOpen: (id: string) => void
}) {
  if (!albums.length) return null
  return (
    <>
      <div className="section-title">{title}</div>
      <div className="grid">
        {albums.map((a) => (
          <button type="button" key={a.id} className="card album-card" onClick={() => onOpen(a.id)} aria-label={`${a.name} — ${a.artist}`}>
            <Cover id={a.coverArt} />
            <div className="card-title">{a.name}</div>
            <div className="card-sub">{a.artist}</div>
          </button>
        ))}
      </div>
    </>
  )
}
