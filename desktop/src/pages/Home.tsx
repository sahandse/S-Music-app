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
    let active = true
    Promise.all([
      getAlbumList(creds, 'newest', 18),
      getAlbumList(creds, 'frequent', 18),
      getRandomSongs(creds, 20)
    ])
      .then(([n, f, r]) => {
        if (!active) return
        setRecent(n)
        setFrequent(f)
        setRandom(r)
      })
      .catch(() => { if (active) setLoadError(true) })
      .finally(() => { if (active) setLoading(false) })
    getGenres(creds)
      .then((g) => {
        if (!active) return
        setGenres(g)
        if (g.length) setGenre(g[0])
      })
      .catch(() => { if (active) setGenres([]) })
    // Latest shared-catalog songs ("Newest Iranian"), refreshed periodically so
    // songs newly posted to the channel appear without a manual reload.
    const loadCatalog = () =>
      fetchCatalog()
        .then((list) => { if (active) setNewSongs(list.slice(0, 12).map(catalogToSong)) })
        .catch(() => {})
    loadCatalog()
    const id = setInterval(loadCatalog, 60000)
    return () => { active = false; clearInterval(id) }
  }, [creds, retry])

  useEffect(() => {
    if (!creds || !genre) {
      setGenreSongs([])
      return
    }
    let active = true
    setGenreSongs([])
    getSongsByGenre(creds, genre, 20)
      .then(songs => { if (active) setGenreSongs(songs) })
      .catch(() => { if (active) setGenreSongs([]) })
    return () => { active = false }
  }, [creds, genre])

  if (loading) return <div className="home-loading" role="status" aria-label={t('common.loading')}><div className="skeleton-heading" /> <div className="grid">{Array.from({ length: 8 }, (_, i) => <div className="skeleton-card" key={i}><div className="skeleton-cover"/><div className="skeleton-line"/><div className="skeleton-line short"/></div>)}</div><span className="sr-only">{t('common.loading')}</span></div>

  return (
    <div>
      <header className="home-head"><h1 className="page-title">{t('nav.home')}</h1><button type="button" className="home-search-btn" onClick={() => navigate('search')}>{t('nav.search')} <span aria-hidden="true">⌕</span></button></header>
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

      {random.length > 0 && <div className="section-title">
        {t('home.random')}{' '}
        <button
          className="chip active"
          style={{ marginInlineStart: 12 }}
          onClick={() => playQueue(random, 0)}
        >
          {t('common.playAll')}
        </button>
      </div>
      {random.length > 0 && <TrackList songs={random} />}

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
      <h2 className="section-title">{title}</h2>
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
