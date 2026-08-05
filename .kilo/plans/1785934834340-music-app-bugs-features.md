# Music App — Bug Fix & Feature Improvement Plan

## Overview

Two music apps found in the workspace:
- **High Music** — Windows desktop (Electron + React + TypeScript) at `desktop/`
- **S-Music App** — Android mobile (Kotlin + Jetpack Compose + Media3) at `app/`

Both connect to a Subsonic/Navidrome server.

---

## Critical Bugs to Fix

### Android (`app/`)
1. **Logout starts playback instead of stopping** — `MusicViewModel.kt:443` calls `togglePlayPause()` on logout. If paused, it starts playing. Fix: replace with `stopPlayback()`.
2. **Hardcoded MIME type `audio/mpeg`** — `MusicPlaybackService.kt:196` forces MP3 MIME type on all media. FLAC/OGG/M4A playback may fail. Fix: use correct MIME type from metadata or let ExoPlayer auto-detect.
3. **Electron deprecated `closed` event** — `desktop/electron/main.js:57` uses deprecated event; switch to `close`/`destroyed`.
4. **31 empty `catch` blocks** — Silent failures across ViewModel, screens, playback, and repository.

### Desktop (`desktop/`)
5. **Non-reactive settings in Login** — `Login.tsx:20` reads settings synchronously; won't react to external changes.
6. **Custom song ID collision** — `customSongs.ts:33,55` uses `Date.now()` + 4-char random suffix in tight loop.
7. **Fade step count asymmetry** — fade-in uses 20 steps, fade-out uses 21.

---

## Feature Suggestions

### High Priority
- **Queue management improvements** — drag-to-reorder, save queues as playlists, clear queue
- **Lyrics sync with timestamps** — Android already fetches lyrics; add auto-scroll and karaoke-style highlighting
- **Offline mode for desktop** — currently requires server; add local file playback for imported songs
- **Sleep timer polish** — fade out + auto-pause vs hard stop option

### Medium Priority
- **Artist/Album discography grouping** — handle multi-disc releases better
- **Search history** — save recent searches per session
- **Widget for desktop** — mini-player already exists; add system tray media controls for non-Electron OS features

### Nice to Have
- **Crossfade visualization** — show overlap preview in settings
- **Smart shuffle** — weighted shuffle (prefer favorites, recently played)
- **Backup/restore playlists** — Android supports backups; extend to desktop settings export

---

## Next Decision

What should I prioritize?
1. **Fix all critical bugs** (no feature additions)
2. **Fix critical bugs + add one high-priority feature**
3. **Only feature improvements** (defer bugs)
4. **Fix a specific subset** — tell me which
