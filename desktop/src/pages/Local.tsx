import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Song } from '../api'
import { bridge, isDesktop } from '../desktop'
import { Cover } from '../components/common'
import { t } from '../i18n'

const KEY = 'highmusic.localFiles'

interface LocalItem {
  id: string
  title: string
  path: string
  url: string
}

function load(): LocalItem[] {
  try {
    return JSON.parse(localStorage.getItem(KEY) || '[]')
  } catch {
    return []
  }
}
function save(list: LocalItem[]) {
  localStorage.setItem(KEY, JSON.stringify(list))
}

function toSong(it: LocalItem): Song {
  return { id: 'local_' + it.id, title: it.title, artist: t('local.fileSource'), directUrl: it.url }
}

export function Local() {
  const { playQueue, addToQueue } = useApp()
  const [items, setItems] = useState<LocalItem[]>(load())

  useEffect(() => save(items), [items])

  const pick = async () => {
    const b = bridge()
    if (!b) return
    const files = await b.pickAudio()
    if (!files.length) return
    const next = [
      ...files.map((f) => ({
        id: f.path.replace(/[^a-z0-9]/gi, '').slice(-16) + Date.now().toString(36),
        title: f.name,
        path: f.path,
        url: f.url
      })),
      ...items
    ]
    setItems(next)
  }

  const remove = (id: string) => setItems((l) => l.filter((x) => x.id !== id))

  if (!isDesktop()) {
    return (
      <div className="page">
        <h1>{t('local.title')}</h1>
        <div className="empty">{t('local.desktopOnly')}</div>
      </div>
    )
  }

  const songs = items.map(toSong)

  return (
    <div className="page">
      <div className="page-head">
        <h1>{t('local.title')}</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          {items.length > 0 && (
            <button className="btn-sm" onClick={() => playQueue(songs, 0)}>
              ▶ {t('common.playAll')}
            </button>
          )}
          <button className="btn-sm" onClick={pick}>
            + {t('local.addFile')}
          </button>
        </div>
      </div>
      {items.length === 0 && <div className="empty">{t('local.empty')}</div>}
      <div>
        {items.map((it, i) => (
          <div key={it.id} className="track-row">
            <div className="t-index" onClick={() => playQueue(songs, i)}>
              {i + 1}
            </div>
            <div className="t-title" onClick={() => playQueue(songs, i)}>
              {it.title}
              <div className="card-sub" style={{ marginTop: 2, direction: 'ltr' }}>
                {it.path}
              </div>
            </div>
            <div className="t-star" title={t('player.addToQueue')} onClick={() => addToQueue([toSong(it)])}>
              ＋
            </div>
            <div className="t-star" title={t('player.remove')} onClick={() => remove(it.id)}>
              ✕
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
