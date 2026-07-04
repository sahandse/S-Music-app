import React, { useState } from 'react'
import { useApp } from './store'
import { Login } from './pages/Login'
import { Home } from './pages/Home'
import { Albums } from './pages/Albums'
import { AlbumDetail } from './pages/AlbumDetail'
import { Artists } from './pages/Artists'
import { ArtistDetail } from './pages/ArtistDetail'
import { Playlists, PlaylistDetail } from './pages/Playlists'
import { Search } from './pages/Search'
import { Starred } from './pages/Starred'
import { MySongs } from './pages/MySongs'
import { Catalog } from './pages/Catalog'
import { Sidebar } from './components/Sidebar'
import { PlayerBar } from './components/PlayerBar'
import { Queue } from './components/Queue'

export function App() {
  const { creds, view, param } = useApp()
  const [showQueue, setShowQueue] = useState(false)

  if (!creds) return <Login />

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">
        <Content view={view} param={param} />
      </main>
      <PlayerBar onToggleQueue={() => setShowQueue((v) => !v)} />
      {showQueue && <Queue onClose={() => setShowQueue(false)} />}
    </div>
  )
}

function Content({ view, param }: { view: string; param: string | null }) {
  switch (view) {
    case 'home':
      return <Home />
    case 'albums':
      return <Albums />
    case 'album':
      return param ? <AlbumDetail id={param} /> : null
    case 'artists':
      return <Artists />
    case 'artist':
      return param ? <ArtistDetail id={param} /> : null
    case 'playlists':
      return <Playlists />
    case 'playlist':
      return param ? <PlaylistDetail id={param} /> : null
    case 'starred':
      return <Starred />
    case 'search':
      return <Search />
    case 'mysongs':
      return <MySongs />
    case 'catalog':
      return <Catalog />
    default:
      return <Home />
  }
}
