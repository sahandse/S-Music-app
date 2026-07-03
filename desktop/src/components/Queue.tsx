import React from 'react'
import { useApp } from '../store'
import { Cover } from './common'

export function Queue({ onClose }: { onClose: () => void }) {
  const { queue, index, jumpTo, current } = useApp()
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
          className={`queue-item ${current?.id === s.id && i === index ? 'playing' : ''}`}
          onClick={() => jumpTo(i)}
        >
          <Cover id={s.coverArt} size={80} />
          <div style={{ minWidth: 0, flex: 1 }}>
            <div className="p-title">{s.title}</div>
            <div className="card-sub">{s.artist}</div>
          </div>
        </div>
      ))}
    </div>
  )
}
