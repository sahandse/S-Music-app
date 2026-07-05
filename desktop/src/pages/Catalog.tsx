import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Cover } from '../components/common'
import { catalogToSong, CatalogSong, fetchCatalog, dispatchSync, ghToken, setGhToken } from '../catalog'
import { t } from '../i18n'

// Shared online catalog: read a public catalog.json and play direct URLs.
export function Catalog() {
  const { playQueue, current, settings } = useApp()
  void settings.lang
  const [songs, setSongs] = useState<CatalogSong[]>([])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [showAdmin, setShowAdmin] = useState(false)
  const [token, setToken] = useState(ghToken())
  const [syncMsg, setSyncMsg] = useState('')

  const shown = songs

  const doSync = async () => {
    setSyncMsg('')
    setGhToken(token)
    try {
      await dispatchSync()
      setSyncMsg(t('catalog.syncStarted'))
    } catch (e: any) {
      setSyncMsg(e?.message === 'no-token' ? t('catalog.needToken') : t('catalog.syncFailed'))
    }
  }

  const reload = async () => {
    setBusy(true)
    setErr('')
    try {
      setSongs(await fetchCatalog())
    } catch (e: any) {
      setErr(e?.message || t('catalog.error'))
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    reload()
  }, [])

  const playAll = (startIndex: number) => {
    playQueue(shown.map(catalogToSong), startIndex)
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="page-title" style={{ marginBottom: 0 }}>
          🎵 {t('catalog.title')}
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {shown.length > 0 && (
            <button className="chip active" onClick={() => playAll(0)}>
              ▶ {t('common.playAll')}
            </button>
          )}
          <button className="chip" onClick={reload}>
            {t('catalog.refresh')}
          </button>
          <button className="chip" onClick={() => setShowAdmin((v) => !v)} title={t('catalog.admin')}>
            ⚙
          </button>
        </div>
      </div>
      <p className="card-sub" style={{ margin: '8px 0 16px' }}>
        {shown.length} {t('common.songs')} · {t('catalog.subtitle')}
      </p>

      {showAdmin && (
        <div className="settings-card" style={{ maxWidth: 620, marginBottom: 16 }}>
          <h3>{t('catalog.admin')}</h3>
          <p className="card-sub" style={{ marginBottom: 8 }}>{t('catalog.adminHint')}</p>
          <input
            className="inp"
            type="password"
            dir="ltr"
            placeholder="GitHub token"
            value={token}
            onChange={(e) => setToken(e.target.value)}
          />
          <button className="btn-sm" onClick={doSync}>
            🔄 {t('catalog.syncNow')}
          </button>
          {syncMsg && <div className="card-sub" style={{ marginTop: 8 }}>{syncMsg}</div>}
        </div>
      )}

      {busy && songs.length === 0 ? (
        <div className="empty">{t('common.loading')}</div>
      ) : err ? (
        <div className="empty">{err}</div>
      ) : shown.length === 0 ? (
        <div className="empty">{t('catalog.empty')}</div>
      ) : (
        <div>
          {shown.map((s, i) => (
            <div
              key={s.audioUrl}
              className={`track-row ${current?.id === 'cat_' + s.audioUrl ? 'playing' : ''}`}
              style={{ gridTemplateColumns: '48px 1fr 40px' }}
            >
              <div
                onClick={() => playAll(i)}
                style={{ cursor: 'pointer', width: 40, height: 40, borderRadius: 6, overflow: 'hidden' }}
              >
                <Cover directUrl={s.coverUrl} size={80} className="my-song-cover" />
              </div>
              <div className="t-title" onClick={() => playAll(i)} style={{ cursor: 'pointer' }}>
                {s.title}
                <div className="card-sub" style={{ marginTop: 2 }}>
                  {s.artist}
                </div>
              </div>
              <div className="t-index" onClick={() => playAll(i)} style={{ cursor: 'pointer' }}>
                ▶
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
