import React from 'react'
import { useApp } from '../store'

import { isDesktop } from '../desktop'

const NAV = [
  { id: 'home', label: 'خانه', icon: '🏠' },
  { id: 'albums', label: 'آلبوم‌ها', icon: '💿' },
  { id: 'artists', label: 'هنرمندان', icon: '🎤' },
  { id: 'playlists', label: 'پلی‌لیست‌ها', icon: '📃' },
  { id: 'starred', label: 'پسندیده‌ها', icon: '♥' },
  { id: 'search', label: 'جستجو', icon: '🔍' },
  { id: 'catalog', label: 'کاتالوگ آنلاین', icon: '🎵' },
  { id: 'mysongs', label: 'آهنگ‌های من', icon: '➕' },
  ...(isDesktop() ? [{ id: 'local', label: 'آهنگ‌های محلی', icon: '📁' }] : []),
  { id: 'settings', label: 'تنظیمات', icon: '⚙' }
]

export function Sidebar() {
  const { view, navigate, logout, creds } = useApp()
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
          {n.label}
        </div>
      ))}
      <div className="sidebar-footer">
        <div className="nav-sep" />
        <div className="card-sub" style={{ padding: '0 12px 8px' }}>
          {creds?.username}
        </div>
        <div className="nav-item" onClick={logout}>
          <span className="ico">⎋</span> خروج
        </div>
      </div>
    </aside>
  )
}
