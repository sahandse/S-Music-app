import React, { useState } from 'react'
import { useApp } from '../store'
import { Cover } from './common'
import { t } from '../i18n'
import { createPlaylist } from '../api'

export function Queue({ onClose }: { onClose: () => void }) {
  const { queue, index, jumpTo, current, reorderQueue, removeFromQueue, clearQueue, creds } = useApp()
  const [drag, setDrag] = useState<number | null>(null)
  const [over, setOver] = useState<number | null>(null)
  const [saved, setSaved] = useState('')

  const saveAsPlaylist = async () => {
    if (!creds || queue.length === 0) return
    // Only server tracks can be saved into a server playlist
    const ids = queue.filter((s) => !s.directUrl).map((s) => s.id)
    if (ids.length === 0) {
      setSaved(t('queue.saveLocalErr'))
      return
    }
    const name = 'High Music — ' + new Date().toLocaleString()
    try {
      await createPlaylist(creds, name, ids)
      setSaved(t('queue.saved'))
    } catch {
      setSaved(t('queue.saveErr'))
    }
    setTimeout(() => setSaved(''), 3000)
  }

  const drop = (to: number) => {
    if (drag !== null && drag !== to) reorderQueue(drag, to)
    setDrag(null)
    setOver(null)
  }

  return (
    <div className="queue-panel">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>{t('player.queue')}</h2>
        <button style={{ fontSize: 18, color: 'var(--text-dim)' }} onClick={onClose}>
          ✕
        </button>
      </div>
      {queue.length > 0 && (
        <div className="queue-actions">
          <button onClick={saveAsPlaylist}>💾 {t('queue.save')}</button>
          <button onClick={clearQueue}>🗑 {t('queue.clear')}</button>
        </div>
      )}
      {saved && <div className="card-sub" style={{ padding: '0 0 8px' }}>{saved}</div>}
      {queue.length === 0 && <div className="empty">{t('player.emptyQueue')}</div>}
      {queue.map((s, i) => (
        <div
          key={s.id + i}
          draggable
          onDragStart={() => setDrag(i)}
          onDragOver={(e) => {
            e.preventDefault()
            setOver(i)
          }}
          onDrop={() => drop(i)}
          onDragEnd={() => {
            setDrag(null)
            setOver(null)
          }}
          className={`queue-item ${current?.id === s.id && i === index ? 'playing' : ''} ${
            over === i ? 'drop-over' : ''
          }`}
          onClick={() => jumpTo(i)}
        >
          <span className="q-grip" title={t('player.dragReorder')}>⠿</span>
          <Cover id={s.coverArt} directUrl={s.coverImageUrl} size={80} />
          <div style={{ minWidth: 0, flex: 1 }}>
            <div className="p-title">{s.title}</div>
            <div className="card-sub">{s.artist}</div>
          </div>
          <button
            className="q-remove"
            title={t('player.removeFromQueue')}
            onClick={(e) => {
              e.stopPropagation()
              removeFromQueue(i)
            }}
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}
