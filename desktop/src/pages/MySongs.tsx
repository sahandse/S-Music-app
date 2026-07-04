import React, { useState } from 'react'
import { useApp } from '../store'
import { Cover } from '../components/common'
import { fmtDuration } from '../api'
import {
  addCustomSong,
  CustomSong,
  importCustomSongs,
  loadCustomSongs,
  removeCustomSong,
  toSong
} from '../customSongs'
import { t } from '../i18n'

// User-added songs: paste a cover URL, artist, title, and a direct audio link.
// Everything is stored locally and plays through the normal player.
export function MySongs() {
  const { playQueue, current, settings } = useApp()
  void settings.lang
  const [list, setList] = useState<CustomSong[]>(() => loadCustomSongs())

  const [title, setTitle] = useState('')
  const [artist, setArtist] = useState('')
  const [cover, setCover] = useState('')
  const [url, setUrl] = useState('')
  const [msg, setMsg] = useState('')
  const [showImport, setShowImport] = useState(false)
  const [importText, setImportText] = useState('')

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setMsg('')
    if (!title.trim() || !url.trim()) {
      setMsg(t('my.minRequired'))
      return
    }
    const next = addCustomSong({
      title: title.trim(),
      artist: artist.trim(),
      coverImageUrl: cover.trim(),
      directUrl: url.trim()
    })
    setList(next)
    setTitle('')
    setArtist('')
    setCover('')
    setUrl('')
    setMsg(t('my.added'))
  }

  const doImport = () => {
    try {
      const next = importCustomSongs(importText)
      setList(next)
      setImportText('')
      setShowImport(false)
      setMsg(t('my.listImported'))
    } catch (err: any) {
      setMsg(t('my.importErr') + (err?.message || ''))
    }
  }

  const songs = list.map(toSong)

  return (
    <div>
      <div className="page-title">{t('my.title')}</div>
      <p className="card-sub" style={{ marginBottom: 20, maxWidth: 620 }}>
        {t('my.desc')}
      </p>

      <form
        onSubmit={submit}
        style={{
          background: 'var(--bg-elevated)',
          borderRadius: 14,
          padding: 20,
          maxWidth: 620,
          marginBottom: 28
        }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div className="field">
            <label>{t('my.songName')} *</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="field">
            <label>{t('my.artistName')}</label>
            <input value={artist} onChange={(e) => setArtist(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>{t('my.coverLink')}</label>
          <input value={cover} onChange={(e) => setCover(e.target.value)} dir="ltr" placeholder="https://…/cover.jpg" />
        </div>
        <div className="field">
          <label>{t('my.directLink')} *</label>
          <input value={url} onChange={(e) => setUrl(e.target.value)} dir="ltr" placeholder="https://…/song.mp3" />
        </div>
        <div className="row-actions">
          <button className="btn" type="submit">
            + {t('my.addSong')}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => setShowImport((v) => !v)}
          >
            {t('my.bulkImport')}
          </button>
          {songs.length > 0 && (
            <button className="btn ghost" type="button" onClick={() => playQueue(songs, 0)}>
              ▶ {t('common.playAll')}
            </button>
          )}
        </div>
        {msg && <div style={{ marginTop: 10, color: 'var(--accent)', fontSize: 13 }}>{msg}</div>}

        {showImport && (
          <div style={{ marginTop: 16 }}>
            <label className="card-sub">
              آرایه JSON — هر آیتم: {'{'} "title", "artist", "coverImageUrl", "directUrl" {'}'}
            </label>
            <textarea
              value={importText}
              onChange={(e) => setImportText(e.target.value)}
              dir="ltr"
              rows={6}
              style={{
                width: '100%',
                marginTop: 8,
                background: 'var(--bg)',
                border: '1px solid var(--border)',
                borderRadius: 10,
                padding: 12,
                color: 'var(--text)',
                fontFamily: 'monospace',
                fontSize: 12
              }}
              placeholder='[{"title":"...","artist":"...","coverImageUrl":"...","directUrl":"..."}]'
            />
            <button className="btn" type="button" style={{ marginTop: 8 }} onClick={doImport}>
              {t('my.import')}
            </button>
          </div>
        )}
      </form>

      <div className="section-title">
        {t('my.list')} ({list.length})
      </div>
      {list.length === 0 ? (
        <div className="empty">{t('my.empty')}</div>
      ) : (
        <div>
          {list.map((c, i) => (
            <div
              key={c.id}
              className={`track-row ${current?.id === c.id ? 'playing' : ''}`}
              style={{ gridTemplateColumns: '48px 1fr 1fr 40px' }}
            >
              <div
                onClick={() => playQueue(songs, i)}
                style={{ cursor: 'pointer', width: 40, height: 40, borderRadius: 6, overflow: 'hidden' }}
              >
                <Cover directUrl={c.coverImageUrl} size={80} className="my-song-cover" />
              </div>
              <div className="t-title" onClick={() => playQueue(songs, i)} style={{ cursor: 'pointer' }}>
                {c.title}
                <div className="card-sub" style={{ marginTop: 2 }}>
                  {c.artist}
                </div>
              </div>
              <div className="t-album" dir="ltr" style={{ fontSize: 11 }}>
                {c.directUrl}
              </div>
              <div
                className="t-star"
                style={{ opacity: 1 }}
                title={t('player.remove')}
                onClick={() => setList(removeCustomSong(c.id))}
              >
                🗑
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
