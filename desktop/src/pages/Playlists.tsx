import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { getPlaylist, getPlaylists, streamUrl, Playlist, Song, fmtDuration } from '../api'
import { Cover, TrackList } from '../components/common'
import { bridge, isDesktop } from '../desktop'
import { t } from '../i18n'

export function Playlists() {
  const { creds, navigate } = useApp()
  const [playlists, setPlaylists] = useState<Playlist[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getPlaylists(creds)
      .then(setPlaylists)
      .catch(() => setPlaylists([]))
      .finally(() => setLoading(false))
  }, [creds])

  return (
    <div>
      <div className="page-title">{t('nav.playlists')}</div>
      {loading ? (
        <div className="empty">{t('common.loading')}</div>
      ) : playlists.length === 0 ? (
        <div className="empty">{t('playlists.none')}</div>
      ) : (
        <div className="grid">
          {playlists.map((p) => (
            <div key={p.id} className="card" onClick={() => navigate('playlist', p.id)}>
              <Cover id={p.coverArt} />
              <div className="card-title">{p.name}</div>
              <div className="card-sub">
                {p.songCount || 0} {t('common.songs')}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export function PlaylistDetail({ id }: { id: string }) {
  const { creds, playQueue } = useApp()
  const [playlist, setPlaylist] = useState<Playlist | null>(null)
  const [songs, setSongs] = useState<Song[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getPlaylist(creds, id)
      .then(({ playlist, songs }) => {
        setPlaylist(playlist)
        setSongs(songs)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [creds, id])

  if (loading) return <div className="empty">{t('common.loading')}</div>
  if (!playlist) return <div className="empty">{t('playlist.notFound')}</div>

  const total = songs.reduce((a, s) => a + (s.duration || 0), 0)

  const exportM3u = () => {
    if (!creds) return
    let out = '#EXTM3U\n'
    for (const s of songs) {
      const url = s.directUrl || streamUrl(creds, s.id)
      out += `#EXTINF:${Math.round(s.duration || 0)},${s.artist || ''} - ${s.title}\n${url}\n`
    }
    const name = (playlist.name || 'playlist').replace(/[\\/:*?"<>|]/g, '_') + '.m3u'
    const b = bridge()
    if (b) {
      b.saveText(name, out)
    } else {
      const blob = new Blob([out], { type: 'audio/x-mpegurl' })
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = name
      a.click()
    }
  }

  return (
    <div>
      <div className="detail-head">
        <Cover id={playlist.coverArt} size={400} />
        <div className="detail-meta">
          <div className="kind">{t('playlist.kind')}</div>
          <h1>{playlist.name}</h1>
          <div className="sub">
            {songs.length} {t('common.songs')} · {fmtDuration(total)}
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn" onClick={() => playQueue(songs, 0)}>
              ▶ {t('common.play')}
            </button>
            <button className="btn ghost" onClick={exportM3u}>
              {isDesktop() ? t('playlist.exportM3u') : t('playlist.downloadM3u')}
            </button>
          </div>
        </div>
      </div>
      <TrackList songs={songs} />
    </div>
  )
}
