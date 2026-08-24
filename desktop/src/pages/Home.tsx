import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbumList, getGenres, getRandomSongs, getSongsByGenre, Song } from '../api'
import { Cover } from '../components/common'
import { MusicVideoCard } from '../components/MusicVideoCard'
import { catalogToSong, fetchCatalog } from '../catalog'
import { fetchMusicVideos, MusicVideoItem } from '../musicVideos'
import { t } from '../i18n'

// Home is a quick, scannable overview — every section is a single
// horizontal shelf (cover + title + artist), never a long vertical list.
// Full browsing lives on the dedicated Albums/Artists/Catalog/Music Videos
// pages; this page is just the "what's here" summary.
export function Home() {
  const { creds, navigate, playQueue, setFullscreen, settings } = useApp()
  void settings.lang
  const [recent, setRecent] = useState<Album[]>([])
  const [frequent, setFrequent] = useState<Album[]>([])
  const [random, setRandom] = useState<Song[]>([])
  const [newSongs, setNewSongs] = useState<Song[]>([])
  const [videos, setVideos] = useState<MusicVideoItem[]>([])
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
    // Latest shared-catalog songs ("Newest Iranian"), refreshed periodically so
    // songs newly posted to the channel appear without a manual reload.
    const loadCatalog = () =>
      fetchCatalog()
        .then((list) => setNewSongs(list.slice(0, 12).map(catalogToSong)))
        .catch(() => {})
    loadCatalog()
    const catalogId = setInterval(loadCatalog, 60000)
    fetchMusicVideos()
      .then((list) => setVideos(list.slice(0, 12)))
      .catch(() => {})
    return () => clearInterval(catalogId)
  }, [creds])

  useEffect(() => {
    if (!creds || !genre) return
    getSongsByGenre(creds, genre, 12)
      .then(setGenreSongs)
      .catch(() => setGenreSongs([]))
  }, [creds, genre])

  if (loading) return <div className="empty">{t('common.loading')}</div>

  const openAndPlay = (songs: Song[], i: number) => {
    playQueue(songs, i)
    setFullscreen(true)
  }

  return (
    <div>
      <div className="page-title">{t('nav.home')}</div>

      <SongShelf
        title={t('home.newestIranian')}
        songs={newSongs}
        onPlay={(i) => openAndPlay(newSongs, i)}
      />

      {videos.length > 0 && (
        <>
          <div className="section-head">
            <div className="section-title">{t('home.musicVideos')}</div>
            <button className="chip" onClick={() => navigate('musicVideos')}>
              {t('home.seeAll')}
            </button>
          </div>
          <div className="hrow">
            {videos.map((v) => (
              <MusicVideoCard
                key={v.videoUrl}
                video={v}
                onClick={() => navigate('musicVideos')}
                className="hrow-video"
              />
            ))}
          </div>
        </>
      )}

      <AlbumShelf title={t('home.newest')} albums={recent} onOpen={(id) => navigate('album', id)} />
      <AlbumShelf title={t('home.frequent')} albums={frequent} onOpen={(id) => navigate('album', id)} />

      <SongShelf
        title={t('home.random')}
        songs={random}
        onPlay={(i) => openAndPlay(random, i)}
      />

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
          <SongShelf songs={genreSongs} onPlay={(i) => openAndPlay(genreSongs, i)} />
        </>
      )}
    </div>
  )
}

function SongShelf({
  title,
  songs,
  onPlay
}: {
  title?: string
  songs: Song[]
  onPlay: (i: number) => void
}) {
  if (!songs.length) return null
  return (
    <>
      {title && (
        <div className="section-head">
          <div className="section-title">{title}</div>
          <button className="chip" onClick={() => onPlay(0)}>
            ▶ {t('common.playAll')}
          </button>
        </div>
      )}
      <div className="hrow">
        {songs.map((s, i) => (
          <div key={s.id + i} className="hrow-song" onClick={() => onPlay(i)}>
            <Cover id={s.coverArt} directUrl={s.coverImageUrl} />
            <div className="card-title" style={{ marginTop: 8 }}>
              {s.title}
            </div>
            <div className="card-sub">{s.artist}</div>
          </div>
        ))}
      </div>
    </>
  )
}

function AlbumShelf({
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
      <div className="hrow">
        {albums.map((a) => (
          <div key={a.id} className="card hrow-album" onClick={() => onOpen(a.id)}>
            <Cover id={a.coverArt} />
            <div className="card-title">{a.name}</div>
            <div className="card-sub">{a.artist}</div>
          </div>
        ))}
      </div>
    </>
  )
}
