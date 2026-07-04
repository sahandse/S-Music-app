import React, { useState, useEffect } from 'react'
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
import { Local } from './pages/Local'
import { Settings } from './pages/Settings'
import { Sidebar } from './components/Sidebar'
import { PlayerBar } from './components/PlayerBar'
import { Queue } from './components/Queue'
import { NowPlaying } from './components/NowPlaying'
import { MiniPlayer } from './components/MiniPlayer'

export function App() {
  const { creds, view, param, fullscreen, mini } = useApp()
  const [showQueue, setShowQueue] = useState(false)
  useKeyboardShortcuts()

  if (!creds) return <Login />

  if (mini) return <MiniPlayer />

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">
        <Content view={view} param={param} />
      </main>
      <PlayerBar onToggleQueue={() => setShowQueue((v) => !v)} />
      {showQueue && <Queue onClose={() => setShowQueue(false)} />}
      {fullscreen && <NowPlaying />}
      {/* Escape handling for the overlay is wired in the shortcuts hook */}
      <span style={{ display: 'none' }} data-nav={view} />
    </div>
  )
}

function useKeyboardShortcuts() {
  const {
    togglePlay,
    next,
    prev,
    seek,
    progress,
    volume,
    setVolume,
    fullscreen,
    setFullscreen,
    navigate
  } = useApp()

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement
      const typing =
        t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || (t as any).isContentEditable)
      if (e.ctrlKey && (e.key === 'f' || e.key === 'F')) {
        e.preventDefault()
        navigate('search')
        return
      }
      if (typing) return
      switch (e.key) {
        case ' ':
          e.preventDefault()
          togglePlay()
          break
        case 'ArrowRight':
          seek(progress + 5)
          break
        case 'ArrowLeft':
          seek(Math.max(0, progress - 5))
          break
        case 'ArrowUp':
          e.preventDefault()
          setVolume(Math.min(1, volume + 0.05))
          break
        case 'ArrowDown':
          e.preventDefault()
          setVolume(Math.max(0, volume - 0.05))
          break
        case 'n':
        case 'N':
          next()
          break
        case 'p':
        case 'P':
          prev()
          break
        case 'Escape':
          if (fullscreen) setFullscreen(false)
          break
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [togglePlay, next, prev, seek, progress, volume, setVolume, fullscreen, setFullscreen, navigate])
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
    case 'local':
      return <Local />
    case 'settings':
      return <Settings />
    default:
      return <Home />
  }
}
