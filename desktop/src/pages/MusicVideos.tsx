import React, { useEffect, useMemo, useState } from 'react'
import { fetchMusicVideos, MusicVideoItem } from '../musicVideos'
import { MusicVideoCard } from '../components/MusicVideoCard'
import { t } from '../i18n'

export function MusicVideos() {
  const [videos, setVideos] = useState<MusicVideoItem[]>([])
  const [busy, setBusy] = useState(false)
  const [query, setQuery] = useState('')
  const [genre, setGenre] = useState<string | null>(null)
  const [playing, setPlaying] = useState<MusicVideoItem | null>(null)

  const reload = async () => {
    setBusy(true)
    try {
      setVideos(await fetchMusicVideos())
    } finally {
      setBusy(false)
    }
  }
  useEffect(() => {
    reload()
  }, [])

  const genres = useMemo(
    () => Array.from(new Set(videos.map((v) => v.genre.trim()).filter(Boolean))).sort(),
    [videos]
  )

  const shown = useMemo(
    () =>
      videos.filter((v) => {
        const genreOk = !genre || v.genre.toLowerCase() === genre.toLowerCase()
        const q = query.trim().toLowerCase()
        const queryOk = !q || v.title.toLowerCase().includes(q) || v.artist.toLowerCase().includes(q)
        return genreOk && queryOk
      }),
    [videos, genre, query]
  )

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="page-title" style={{ marginBottom: 0 }}>
          🎬 {t('nav.musicVideos')}
        </div>
        <button className="chip" onClick={reload}>
          {t('catalog.refresh')}
        </button>
      </div>
      <p className="card-sub" style={{ margin: '8px 0 16px' }}>
        {shown.length} {t('mv.count')}
      </p>

      <input
        className="search-input"
        placeholder={t('mv.search')}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      {genres.length > 0 && (
        <div style={{ margin: '14px 0' }}>
          <span className={`chip ${!genre ? 'active' : ''}`} onClick={() => setGenre(null)}>
            {t('catalog.all')}
          </span>
          {genres.map((g) => (
            <span
              key={g}
              className={`chip ${genre === g ? 'active' : ''}`}
              onClick={() => setGenre(genre === g ? null : g)}
            >
              {g}
            </span>
          ))}
        </div>
      )}

      {busy && videos.length === 0 ? (
        <div className="empty">{t('common.loading')}</div>
      ) : shown.length === 0 ? (
        <div className="empty">{t('mv.empty')}</div>
      ) : (
        <div className="mv-grid">
          {shown.map((v) => (
            <MusicVideoCard key={v.videoUrl} video={v} onClick={() => setPlaying(v)} />
          ))}
        </div>
      )}

      {playing && <MusicVideoPlayer video={playing} onClose={() => setPlaying(null)} />}
    </div>
  )
}

function MusicVideoPlayer({ video, onClose }: { video: MusicVideoItem; onClose: () => void }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="mv-player-overlay" onClick={onClose}>
      <div className="mv-player-inner" onClick={(e) => e.stopPropagation()}>
        <video src={video.videoUrl} autoPlay controls className="mv-player-video" />
        <button className="mv-player-close" onClick={onClose} title={t('player.close')}>
          ✕
        </button>
        <div className="mv-player-info">
          <div className="card-title">{video.title || t('mv.untitled')}</div>
          {(video.artist || video.genre) && (
            <div className="card-sub">{[video.artist, video.genre].filter(Boolean).join(' · ')}</div>
          )}
        </div>
      </div>
    </div>
  )
}
