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

  useEffect(() => {
    if (!creds) return
    setLoading(true)
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
      .catch(() => {})
      .finally(() => setLoading(false))
    getGenres(creds)
      .then((g) => {
        setGenres(g)
        if (g.length) setGenre(g[0])
      })
      .catch(() => setGenres([]))
    // Latest shared-catalog songs ("New songs")
    fetchCatalog()
      .then((list) => setNewSongs(list.slice(0, 12).map(catalogToSong)))
      .catch(() => setNewSongs([]))
  }, [creds])

  useEffect(() => {
    if (!creds || !genre) return
    getSongsByGenre(creds, genre, 20)
      .then(setGenreSongs)
      .catch(() => setGenreSongs([]))
  }, [creds, genre])

  if (loading) return <div className="empty">{t('common.loading')}</div>

  return (
    <div>
      <div className="page-title">{t('nav.home')}</div>

      {newSongs.length > 0 && (
        <>
          <div className="section-title">
            {t('home.newSongs')}{' '}
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
              <span
                key={g}
                className={`chip ${genre === g ? 'active' : ''}`}
                onClick={() => setGenre(g)}
              >
                {g}
              </span>
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
          <div key={a.id} className="card" onClick={() => onOpen(a.id)}>
            <Cover id={a.coverArt} />
            <div className="card-title">{a.name}</div>
            <div className="card-sub">{a.artist}</div>
          </div>
        ))}
      </div>
    </>
  )
}
