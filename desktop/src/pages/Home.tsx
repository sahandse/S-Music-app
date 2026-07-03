import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbumList, getRandomSongs, Song } from '../api'
import { Cover, TrackList } from '../components/common'

export function Home() {
  const { creds, navigate, playQueue } = useApp()
  const [recent, setRecent] = useState<Album[]>([])
  const [frequent, setFrequent] = useState<Album[]>([])
  const [random, setRandom] = useState<Song[]>([])
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
  }, [creds])

  if (loading) return <div className="empty">در حال بارگذاری...</div>

  return (
    <div>
      <div className="page-title">خانه</div>

      <AlbumRow title="تازه‌ترین‌ها" albums={recent} onOpen={(id) => navigate('album', id)} />
      <AlbumRow title="پرشنیده‌ها" albums={frequent} onOpen={(id) => navigate('album', id)} />

      <div className="section-title">
        آهنگ‌های تصادفی{' '}
        <button
          className="chip active"
          style={{ marginRight: 12 }}
          onClick={() => playQueue(random, 0)}
        >
          پخش همه
        </button>
      </div>
      <TrackList songs={random} />
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
