import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbumList } from '../api'
import { Cover } from '../components/common'

const SORTS = [
  { id: 'newest', label: 'تازه‌ترین' },
  { id: 'frequent', label: 'پرشنیده' },
  { id: 'recent', label: 'اخیراً پخش‌شده' },
  { id: 'alphabeticalByName', label: 'الفبا' },
  { id: 'random', label: 'تصادفی' }
]

export function Albums() {
  const { creds, navigate } = useApp()
  const [sort, setSort] = useState('newest')
  const [albums, setAlbums] = useState<Album[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getAlbumList(creds, sort, 100)
      .then(setAlbums)
      .catch(() => setAlbums([]))
      .finally(() => setLoading(false))
  }, [creds, sort])

  return (
    <div>
      <div className="page-title">آلبوم‌ها</div>
      <div style={{ marginBottom: 20 }}>
        {SORTS.map((s) => (
          <span
            key={s.id}
            className={`chip ${sort === s.id ? 'active' : ''}`}
            onClick={() => setSort(s.id)}
          >
            {s.label}
          </span>
        ))}
      </div>
      {loading ? (
        <div className="empty">در حال بارگذاری...</div>
      ) : (
        <div className="grid">
          {albums.map((a) => (
            <div key={a.id} className="card" onClick={() => navigate('album', a.id)}>
              <Cover id={a.coverArt} />
              <div className="card-title">{a.name}</div>
              <div className="card-sub">{a.artist}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
