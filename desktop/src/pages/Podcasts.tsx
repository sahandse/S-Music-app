import React, { useState } from 'react'
import { useApp } from '../store'
import { fmtDuration } from '../api'
import { Episode, Podcast, episodeToSong, fetchEpisodes, searchPodcasts } from '../podcasts'
import { t } from '../i18n'

export function Podcasts() {
  const { playQueue, current, settings } = useApp()
  void settings.lang
  const [q, setQ] = useState('')
  const [results, setResults] = useState<Podcast[]>([])
  const [selected, setSelected] = useState<Podcast | null>(null)
  const [episodes, setEpisodes] = useState<Episode[]>([])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const doSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!q.trim()) return
    setBusy(true)
    setErr('')
    setSelected(null)
    setEpisodes([])
    try {
      setResults(await searchPodcasts(q.trim()))
    } catch {
      setErr(t('pod.searchErr'))
    } finally {
      setBusy(false)
    }
  }

  const openPod = async (p: Podcast) => {
    setSelected(p)
    setBusy(true)
    setErr('')
    try {
      setEpisodes(await fetchEpisodes(p))
    } catch {
      setErr(t('pod.feedErr'))
    } finally {
      setBusy(false)
    }
  }

  const playFrom = (i: number) => {
    playQueue(episodes.map(episodeToSong), i)
  }

  return (
    <div className="page">
      <h1>{t('nav.podcasts')}</h1>
      <form onSubmit={doSearch}>
        <input
          className="search-input"
          placeholder={t('pod.placeholder')}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          autoFocus
        />
      </form>

      {busy && <div className="empty">{t('common.loading')}</div>}
      {err && <div className="empty">{err}</div>}

      {!selected && results.length > 0 && (
        <div className="grid" style={{ marginTop: 18 }}>
          {results.map((p) => (
            <div key={p.id} className="card" onClick={() => openPod(p)}>
              {p.artwork ? <img src={p.artwork} alt="" /> : <div className="cover-fallback">🎙</div>}
              <div className="card-title">{p.title}</div>
              <div className="card-sub">{p.author}</div>
            </div>
          ))}
        </div>
      )}

      {selected && (
        <div style={{ marginTop: 10 }}>
          <div className="detail-head">
            {selected.artwork ? (
              <img src={selected.artwork} style={{ width: 160, height: 160, borderRadius: 12 }} alt="" />
            ) : (
              <div className="cover-fallback" style={{ width: 160, height: 160 }}>🎙</div>
            )}
            <div className="detail-meta">
              <div className="kind">{t('nav.podcasts')}</div>
              <h1>{selected.title}</h1>
              <div className="sub">{selected.author}</div>
              <button className="btn ghost" onClick={() => setSelected(null)}>
                ← {t('pod.back')}
              </button>
            </div>
          </div>
          <div style={{ marginTop: 12 }}>
            {episodes.map((ep, i) => {
              const active = current?.id === 'pod_' + ep.audioUrl
              return (
                <div
                  key={ep.audioUrl}
                  className={`track-row ${active ? 'playing' : ''}`}
                  style={{ gridTemplateColumns: '48px 1fr 90px' }}
                  onClick={() => playFrom(i)}
                >
                  <div className="t-index">{active ? '▶' : i + 1}</div>
                  <div className="t-title">
                    {ep.title}
                    <div className="card-sub" style={{ marginTop: 2 }}>
                      {ep.date}
                    </div>
                  </div>
                  <div className="t-dur">{ep.durationSec ? fmtDuration(ep.durationSec) : ''}</div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
