import React, { useState } from 'react'
import { useApp } from '../store'
import { Cover } from './common'

export function Queue({ onClose }: { onClose: () => void }) {
  const { queue, index, jumpTo, current, reorderQueue, removeFromQueue } = useApp()
  const [drag, setDrag] = useState<number | null>(null)
  const [over, setOver] = useState<number | null>(null)

  const drop = (to: number) => {
    if (drag !== null && drag !== to) reorderQueue(drag, to)
    setDrag(null)
    setOver(null)
  }

  return (
    <div className="queue-panel">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>صف پخش</h2>
        <button style={{ fontSize: 18, color: 'var(--text-dim)' }} onClick={onClose}>
          ✕
        </button>
      </div>
      {queue.length === 0 && <div className="empty">صف خالی است</div>}
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
          <span className="q-grip" title="کشیدن برای جابجایی">⠿</span>
          <Cover id={s.coverArt} directUrl={s.coverImageUrl} size={80} />
          <div style={{ minWidth: 0, flex: 1 }}>
            <div className="p-title">{s.title}</div>
            <div className="card-sub">{s.artist}</div>
          </div>
          <button
            className="q-remove"
            title="حذف از صف"
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
