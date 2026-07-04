import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Album, getAlbumList } from '../api'
import { Cover } from '../components/common'
import { t } from '../i18n'

const SORTS = [
  { id: 'newest', key: 'sort.newest' },
  { id: 'frequent', key: 'sort.frequent' },
  { id: 'recent', key: 'sort.recent' },
  { id: 'alphabeticalByName', key: 'sort.alpha' },
  { id: 'random', key: 'sort.random' }
]

export function Albums() {
  const { creds, navigate, settings } = useApp()
  void settings.lang
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
      <div className="page-title">{t('nav.albums')}</div>
      <div style={{ marginBottom: 20 }}>
        {SORTS.map((s) => (
          <span
            key={s.id}
            className={`chip ${sort === s.id ? 'active' : ''}`}
            onClick={() => setSort(s.id)}
          >
            {t(s.key)}
          </span>
        ))}
      </div>
      {loading ? (
        <div className="empty">{t('common.loading')}</div>
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
