# Music App Feature Improvements — Implementation Plan

Items 1–7, 9–12 from improvement list. Item 8 (default server URL) excluded per user request.

---

## 1. Modularize ViewModel and Screens

### Current state
- `MusicViewModel.kt`: 2414 lines, single class handling auth, home, search, settings, downloads, podcasts, Telegram, Apple Music, lyrics, quiz, device songs, etc.
- `MusicScreens.kt`: 11448 lines, all Compose screens in one file.

### Target structure
Split into focused modules:

**ViewModels:**
- `ui/MusicViewModel.kt` → keep only shared navigation state (`_activeServer`, `_isServerConfigured`, `_loginError`, `_language`, `_userName`, `_onboardingPreferencesSet`, `_preferredGenres`, `_downloadLocation`, `_audioQuality`, `_cacheSizeMb`, `_autoBackupEnabled`, `_bassLevel`, `_trebleLevel`, `_crossfadeEnabled`, `_crossfadeDurationSeconds`, `_wifiOnlyDownload`, `_fadeOnPause`, `_resumeOnBluetooth`, `_saveDownloadsPublic`, `pendingShortcutDest`, `allServers`, `_searchFilter`, `_trendingAlbums`, `_searchHistory`).
- `ui/AuthViewModel.kt` — login, logout, register, guest mode, server management.
- `ui/HomeViewModel.kt` — home content, random songs, newest albums, artists, taste mix, discover weekly, remix, music videos, device songs, Apple RSS.
- `ui/SearchViewModel.kt` — search query, results, filters, history, voice search.
- `ui/PlayerViewModel.kt` — playback queue, current track, progress, shuffle, repeat, speed, sleep timer, lyrics, translations.
- `ui/SettingsViewModel.kt` — audio quality, download quality, EQ, crossfade, wifi-only, backup, notifications.
- `ui/LibraryViewModel.kt` — favorites, playlists, recent plays, downloads, Telegram songs, podcasts.

**Screens:**
- `ui/screens/MusicScreens.kt` → split into:
  - `LoginScreen.kt`
  - `OnboardingScreen.kt` (GenrePreferenceScreen)
  - `HomeScreen.kt` (MainMusicApp shell)
  - `SearchScreen.kt`
  - `PlayerScreen.kt` (FullscreenPlayer)
  - `LibraryScreen.kt` (Favorites, Playlists, Downloads, Recent)
  - `DiscoverScreen.kt` (Top charts, Apple feed, video feed)
  - `ProfileScreen.kt`
  - `SettingsScreen.kt`
  - `PodcastsScreen.kt`
  - `DeviceSongsScreen.kt`
  - Common composables extracted to `ui/screens/common/` (SectionTitle, HorizontalMediaCard, VerticalBigCard, etc.)

### Migration strategy
- Extract composables and ViewModel classes one file at a time.
- Keep `MusicViewModel` as a facade that delegates to sub-ViewModels during transition.
- Update imports in `MainActivity` and `AppNavigation` incrementally.

---

## 2. Accessibility: contentDescription audit

### Current state
87 instances of `contentDescription = null` in `MusicScreens.kt`. Many are on interactive icons (play, pause, next, prev, like, queue, settings, search, close, back, etc.).

### Fix
- For decorative icons: add `Modifier.semantics { hidden() }` or keep `null` but document intent.
- For interactive icons: add descriptive `contentDescription = stringResource(R.string.xxx)` or inline translated string.
- Priority icons:
  - Play/Pause/Next/Prev/Skip → "پخش", "مکث", "بعدی", "قبلی"
  - Like/Favorite → "لایک", "علاقه‌مندی"
  - Queue → "صف پخش"
  - Search → "جستجو"
  - Settings → "تنظیمات"
  - Close/Back → "بستن", "برگشت"
  - Car mode → "حالت خودرو"
  - Speed/EQ → "سرعت پخش", "اکولایزر"

### Validation
Run Android accessibility lint: `./gradlew :app:lint` and review `accessibility` warnings.

---

## 3. Android Queue Reorder

### Current state
- Desktop: drag-to-reorder implemented in `Queue.tsx` using HTML5 drag events.
- Android: `showQueueSheet` bottom sheet shows queue but no reorder or remove.

### Implementation
- In `MusicScreens.kt` queue bottom sheet (around line 9088), replace simple `Column` with `LazyColumn`.
- Add `ReorderableCollection` or custom drag using `Modifier.pointerInput` + `detectDragGestures`.
- Track `dragIndex` and `dragOffset` state.
- On drop, call `viewModel.playbackConnection.reorderQueue(dragIndex, targetIndex)`.
- Add swipe-to-remove: `Modifier.swipeToDismiss` calling `removeFromQueue`.
- Visual feedback: elevation/shadow on dragged item, placeholder for drop target.

### Files
- `ui/screens/MusicScreens.kt` (queue bottom sheet section)
- `playback/PlaybackConnection.kt` — ensure `reorderQueue` is exposed (it already exists in desktop store, verify Android side).

---

## 4. Lyrics Karaoke Highlight

### Current state
- `SyncedLyricsView` exists and scrolls to active line.
- Active line is indicated by color/opacity change only.
- Desktop `NowPlaying.tsx` also has basic active line detection.

### Enhancement
- Active line: scale up to 1.08, add text shadow/glow (`shadow` modifier), bold weight, accent color.
- Inactive lines: opacity 0.45, normal scale.
- Smooth transition: `animateFloatAsState` for scale/opacity, 300ms tween.
- Desktop: same treatment in `NowPlaying.tsx` lyrics section.

### Files
- `ui/screens/MusicScreens.kt` (SyncedLyricsView composable)
- `desktop/src/components/NowPlaying.tsx` (lyrics rendering)

---

## 5. Sleep Timer with Fade-Out

### Current state
- `PlaybackConnection` has `startSleepTimer(minutes)` and `cancelSleepTimer()`.
- Behavior: hard stop or end-of-track.
- UI: `SleepTimerDialog` with preset minutes + "end of track".

### Enhancement
- Add fade-out option: before stopping, ramp volume to 0 over configurable duration (3s, 5s, 10s).
- New settings: `sleepFadeDurationMs` in `PlaybackConnection` or `MusicViewModel`.
- In `runFadeOut`, reuse existing fade logic but trigger it from sleep timer countdown when `remainingSeconds <= fadeDuration`.
- UI change: in `SleepTimerDialog`, add segmented control for fade duration: "بدون محو", "۳ ثانیه", "۵ ثانیه", "۱۰ ثانیه".
- Desktop `PlayerBar.tsx` sleep menu: add same fade options.

### Files
- `playback/PlaybackConnection.kt` — add `fadeOutForSleep()` helper
- `ui/screens/MusicScreens.kt` — update `SleepTimerDialog`
- `desktop/src/components/PlayerBar.tsx` — update sleep menu options

---

## 6. Memory Leak Fix in PlaybackConnection

### Current state
```kotlin
private val connectionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
```
This scope is never cancelled. `PlaybackConnection` is instantiated in `ViewModel` init and lives as long as ViewModel, but ViewModel may outlive the Activity/Fragment in some configurations.

### Fix
- Add `fun release()` to `PlaybackConnection` that cancels `connectionScope` and any active jobs (`progressJob`, `sleepTimerJob`).
- Call `release()` from `ViewModel.onCleared()` or `MainActivity.onDestroy()`.
- Alternatively, make `connectionScope` a `CoroutineScope` tied to `viewModelScope` by passing it in, but `PlaybackConnection` is created before ViewModel in some cases. Better to keep explicit `release()`.

### Files
- `playback/PlaybackConnection.kt`
- `ui/MusicViewModel.kt` — call `playbackConnection.release()` in `onCleared()` (if ViewModel has `onCleared` override) or in `MainActivity`.

---

## 7. Move Hardcoded Artist Data to Asset

### Current state
`MusicScreens.kt` lines 5042–5100+ contain hardcoded artist extras (Taylor Swift, The Weeknd, Coldplay, Adele, Eminem, Billie Eilish, Dua Lipa, ...) with social links, country, birth year.

### Fix
- Create `assets/artists.json` (or `res/raw/artists.json`) with structured data:
  ```json
  [
    {
      "name": "Taylor Swift",
      "country": "United States",
      "birthYear": "1989",
      "instagram": "taylorswift",
      "twitter": "taylorswift13",
      "youtube": "TaylorSwift",
      "spotify": "06HL4z00K2zSTw0szgqgA8"
    }
  ]
  ```
- Load once in `MusicViewModel` init or `Application` class, expose as `Map<String, ArtistExtraDetails>`.
- `getArtistExtraDetails()` becomes a simple map lookup.
- Future updates: replace JSON without code change.

### Files
- `app/src/main/assets/artists.json` (new)
- `ui/MusicViewModel.kt` — load asset, replace `when` block
- `ui/screens/MusicScreens.kt` — remove hardcoded block, call ViewModel lookup

---

## 9. Android Equalizer Presets

### Current state
- Desktop has EQ presets (Neutral, Bass, Pop, Rock, Clear) in `Settings.tsx`.
- Android has individual bass/treble sliders only, no presets.

### Implementation
- Add preset definitions in `MusicViewModel` or new `EQPresets.kt`:
  ```kotlin
  data class EQPreset(val name: String, val bands: List<Int>)
  val presets = listOf(
      EQPreset("Natural", listOf(0,0,0,0,0)),
      EQPreset("Bass", listOf(6,4,0,0,2)),
      ...
  )
  ```
- In `SettingsScreen` (currently part of `MusicScreens.kt`), add a `Row` of preset chips.
- On chip click, update `_bassLevel`, `_trebleLevel`, and band values in `Equalizer`.
- Save selected preset name to SharedPreferences.

### Files
- `ui/MusicViewModel.kt` — add preset data + `applyPreset()` function
- `ui/screens/MusicScreens.kt` — preset chip UI in settings section

---

## 10. Desktop Settings Export/Import

### Current state
- Android has backup/restore playlists and settings.
- Desktop settings are stored via `localStorage` and `getSettings()`/`setSettings()` but no export/import.

### Implementation
- In `desktop/src/pages/Settings.tsx`, add two buttons:
  - Export Settings: serialize `getSettings()` + creds (optional) + custom songs to JSON, trigger browser download.
  - Import Settings: file picker, parse JSON, validate schema, call `setSettings()`.
- Format:
  ```json
  {
    "version": 1,
    "settings": { ... },
    "customSongs": [ ... ]
  }
  ```
- Security: warn user that imported file may contain credentials.

### Files
- `desktop/src/pages/Settings.tsx`
- `desktop/src/settings.ts` — add `exportSettings()` / `importSettings()` helpers

---

## 11. Android Recently Played Screen

### Current state
- `PlaybackConnection` calls `repository.insertRecentPlay(song)`.
- `_recentPlays` StateFlow exists in `MusicViewModel`.
- No dedicated screen to browse recent plays.

### Implementation
- Add `RecentlyPlayedScreen` composable showing last N tracks (e.g., 50) from `viewModel.recentPlays`.
- Show cover, title, artist, play date (relative: "2 days ago").
- Tap to play. Swipe-to-remove with undo snackbar.
- Add to NavigationRail as "Recent" tab or as a section in Library.

### Files
- `ui/screens/MusicScreens.kt` — new `RecentlyPlayedScreen` composable
- `ui/MusicViewModel.kt` — ensure `recentPlays` is exposed (verify existing StateFlow)
- `data/repository/MusicRepository.kt` — `getRecentPlays(limit)` query if not exists

---

## 12. Smart Shuffle

### Current state
- Shuffle is uniform random: `queue.shuffled()`.
- No weighting toward favorites, recency, or play count.

### Implementation
- Add `SmartShuffle` utility in `playback/PlaybackConnection.kt` or new `playback/SmartShuffle.kt`:
  ```kotlin
  fun smartShuffle(queue: List<SubsonicSong>, favorites: Set<String>, recentIds: List<String>, playCounts: Map<String, Int>): List<SubsonicSong>
  ```
- Weight formula:
  - Base weight = 1.0
  - If in favorites: +3.0
  - If in recent 20: +2.0
  - If play count > 10: +1.0
  - Normalize weights, sample without replacement.
- Expose as `setSmartShuffleEnabled(boolean)` in `PlaybackConnection`.
- UI: toggle between `Shuffle` (uniform) and `Smart Shuffle` (weighted) in player bar. Use different icon or long-press to switch.

### Files
- `playback/SmartShuffle.kt` (new)
- `playback/PlaybackConnection.kt` — add smart shuffle toggle + apply
- `ui/screens/MusicScreens.kt` — UI toggle
- `desktop/src/components/PlayerBar.tsx` — same toggle for desktop

---

## Validation
- **Android:** `./gradlew :app:assembleDebug` succeeds after each major refactor.
- **Desktop:** `npm run build:renderer` succeeds.
- **Manual:** Verify new screens navigate correctly, queue reorder works, lyrics highlight animates, sleep timer fades, settings export/import round-trips.

## Out of Scope
- Item 8: default server URL change.
- Spotify playlist import fragility (requires ToS review).
- Any additional features beyond the 11 items above.
