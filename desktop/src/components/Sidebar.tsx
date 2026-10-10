import React from 'react'
import { useApp } from '../store'
import { isDesktop } from '../desktop'
import { t } from '../i18n'
import './sidebar-icons.css'

type IconName = 'home' | 'albums' | 'artists' | 'playlists' | 'starred' | 'search' | 'podcasts' | 'catalog' | 'mysongs' | 'local' | 'settings' | 'logout'
const paths: Record<IconName, React.ReactNode> = {
  home: <><path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z"/><path d="M9 21v-8h6v8"/></>,
  albums: <><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/></>,
  artists: <><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></>,
  playlists: <><path d="M4 6h12M4 11h12M4 16h8"/><path d="M18 13v8m-3-3h6"/></>,
  starred: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8z"/>,
  search: <><circle cx="10.8" cy="10.8" r="7"/><path d="m16 16 5 5"/></>,
  podcasts: <><circle cx="12" cy="11" r="2"/><path d="M9 21v-4a3 3 0 0 1 6 0v4M7 15a7 7 0 1 1 10 0M4.5 18a10.5 10.5 0 1 1 15 0"/></>,
  catalog: <><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></>,
  mysongs: <><path d="M4 7h10M4 12h10M4 17h6M19 13v8m-4-4h8"/></>,
  local: <path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v10H3z"/>,
  settings: <><circle cx="12" cy="12" r="3"/><path d="M12 2v3m0 14v3M2 12h3m14 0h3M4.9 4.9 7 7m10 10 2.1 2.1M19.1 4.9 17 7M7 17l-2.1 2.1"/></>,
  logout: <><path d="M10 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h5M14 16l4-4-4-4m4 4H9"/></>
}
function NavIcon({ name }: { name: IconName }) {
  return <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}
const NAV: IconName[] = ['home', 'albums', 'artists', 'playlists', 'starred', 'search', 'podcasts', 'catalog', 'mysongs', ...(isDesktop() ? ['local' as const] : []), 'settings']

export function Sidebar() {
  const { view, navigate, logout, creds, settings } = useApp()
  void settings.lang
  return (
    <aside className="sidebar" aria-label="Music navigation">
      <div className="brand">High<span className="dot">Music</span></div>
      <nav aria-label="Music">
        {NAV.map(id => (
          <button type="button" key={id} className={`nav-item ${view === id ? 'active' : ''}`}
            onClick={() => navigate(id)} aria-current={view === id ? 'page' : undefined}
            title={t('nav.' + id)}>
            <span className="ico"><NavIcon name={id}/></span>
            <span className="nav-label">{t('nav.' + id)}</span>
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        <div className="nav-sep" />
        <div className="card-sub" style={{ padding: '0 12px 8px' }}>{creds?.username}</div>
        <button type="button" className="nav-item" onClick={logout} title={t('nav.logout')}>
          <span className="ico"><NavIcon name="logout" /></span>
          <span className="nav-label">{t('nav.logout')}</span>
        </button>
      </div>
    </aside>
  )
}
