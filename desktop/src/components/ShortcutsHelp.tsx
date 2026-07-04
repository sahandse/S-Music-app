import React from 'react'
import { t } from '../i18n'

const ROWS: Array<[string, string]> = [
  ['Space', 'sc.playPause'],
  ['← / →', 'sc.seek'],
  ['↑ / ↓', 'sc.volume'],
  ['N / P', 'sc.nextPrev'],
  ['Ctrl + F', 'sc.search'],
  ['?', 'sc.help'],
  ['Esc', 'sc.close']
]

export function ShortcutsHelp({ onClose }: { onClose: () => void }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h2>{t('sc.title')}</h2>
          <button onClick={onClose}>✕</button>
        </div>
        <div className="sc-list">
          {ROWS.map(([key, label]) => (
            <div key={key} className="sc-row">
              <span className="sc-desc">{t(label)}</span>
              <kbd className="sc-key">{key}</kbd>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
