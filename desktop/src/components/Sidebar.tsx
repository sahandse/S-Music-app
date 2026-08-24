import React from 'react'
import { useApp } from '../store'
import { isDesktop } from '../desktop'
import { t } from '../i18n'

const NAV = [
  { id: 'home', icon: '🏠' },
  { id: 'albums', icon: '💿' },
  { id: 'artists', icon: '🎤' },
  { id: 'playlists', icon: '📃' },
  { id: 'starred', icon: '♥' },
  { id: 'search', icon: '🔍' },
  { id: 'podcasts', icon: '🎙' },
  { id: 'catalog', icon: '🎵' },
  { id: 'musicVideos', icon: '🎬' },
  { id: 'mysongs', icon: '➕' },
  ...(isDesktop() ? [{ id: 'local', icon: '📁' }] : []),
  { id: 'settings', icon: '⚙' }
]

export function Sidebar() {
  const { view, navigate, logout, creds, settings } = useApp()
  // read settings so the sidebar re-renders when language changes
  void settings.lang
  return (
    <aside className="sidebar">
      <div className="brand">
        High<span className="dot">Music</span>
      </div>
      {NAV.map((n) => (
        <div
          key={n.id}
          className={`nav-item ${view === n.id ? 'active' : ''}`}
          onClick={() => navigate(n.id)}
        >
          <span className="ico">{n.icon}</span>
          {t('nav.' + n.id)}
        </div>
      ))}
      <div className="sidebar-footer">
        <div className="nav-sep" />
        <div className="card-sub" style={{ padding: '0 12px 8px' }}>
          {creds?.username}
        </div>
        <div className="nav-item" onClick={logout}>
          <span className="ico">⎋</span> {t('nav.logout')}
        </div>
      </div>
    </aside>
  )
}
