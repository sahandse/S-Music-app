import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { search, SearchResult } from '../api'
import { Cover, TrackList } from '../components/common'
import { t } from '../i18n'

export function Search() {
  const { creds, navigate, settings } = useApp()
  void settings.lang
  const [q, setQ] = useState('')
  const [res, setRes] = useState<SearchResult | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!creds || !q.trim()) {
      setRes(null)
      return
    }
    const t = setTimeout(() => {
      setLoading(true)
      search(creds, q.trim())
        .then(setRes)
        .catch(() => setRes(null))
        .finally(() => setLoading(false))
    }, 350)
    return () => clearTimeout(t)
  }, [creds, q])

  return (
    <div>
      <div className="page-title">{t('nav.search')}</div>
      <input
        className="search-input"
        placeholder={t('search.placeholder')}
        value={q}
        onChange={(e) => setQ(e.target.value)}
        autoFocus
      />

      {loading && <div className="empty">{t('common.searching')}</div>}

      {res && (
        <div style={{ marginTop: 12 }}>
          {res.artists.length > 0 && (
            <>
              <div className="section-title">{t('search.artists')}</div>
              <div className="grid">
                {res.artists.slice(0, 12).map((a) => (
                  <div key={a.id} className="card round" onClick={() => navigate('artist', a.id)}>
                    <Cover id={a.coverArt} />
                    <div className="card-title">{a.name}</div>
                  </div>
                ))}
              </div>
            </>
          )}

          {res.albums.length > 0 && (
            <>
              <div className="section-title">{t('search.albums')}</div>
              <div className="grid">
                {res.albums.slice(0, 18).map((a) => (
                  <div key={a.id} className="card" onClick={() => navigate('album', a.id)}>
                    <Cover id={a.coverArt} />
                    <div className="card-title">{a.name}</div>
                    <div className="card-sub">{a.artist}</div>
                  </div>
                ))}
              </div>
            </>
          )}

          {res.songs.length > 0 && (
            <>
              <div className="section-title">{t('search.songs')}</div>
              <TrackList songs={res.songs} />
            </>
          )}

          {!res.artists.length && !res.albums.length && !res.songs.length && (
            <div className="empty">{t('common.noResults')}</div>
          )}
        </div>
      )}
    </div>
  )
}
