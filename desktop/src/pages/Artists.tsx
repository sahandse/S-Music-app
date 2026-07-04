import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Artist, getArtists } from '../api'
import { Cover } from '../components/common'
import { t } from '../i18n'

export function Artists() {
  const { creds, navigate, settings } = useApp()
  void settings.lang
  const [artists, setArtists] = useState<Artist[]>([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getArtists(creds)
      .then(setArtists)
      .catch(() => setArtists([]))
      .finally(() => setLoading(false))
  }, [creds])

  const shown = q
    ? artists.filter((a) => a.name.toLowerCase().includes(q.toLowerCase()))
    : artists

  return (
    <div>
      <div className="page-title">{t('nav.artists')}</div>
      <input
        className="search-input"
        placeholder={t('artists.filter')}
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />
      {loading ? (
        <div className="empty">{t('common.loading')}</div>
      ) : (
        <div className="grid" style={{ marginTop: 20 }}>
          {shown.map((a) => (
            <div key={a.id} className="card round" onClick={() => navigate('artist', a.id)}>
              <Cover id={a.coverArt} />
              <div className="card-title">{a.name}</div>
              <div className="card-sub">
                {a.albumCount || 0} {t('album.albums')}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
