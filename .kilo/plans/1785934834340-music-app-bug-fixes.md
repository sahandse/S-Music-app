# Music App Bug Fixes — Implementation Plan

Fix all identified bugs across Android (`app/`) and Desktop (`desktop/`).

---

## Android App (`app/src/main/java/com/example/`)

### 1. Logout starts playback instead of stopping
- **File:** `ui/MusicViewModel.kt:443`
- **Change:** Replace `playbackConnection.togglePlayPause()` with `playbackConnection.stopPlayback()`

### 2. Hardcoded MIME type breaks non-MP3 playback
- **File:** `playback/MusicPlaybackService.kt:196`
- **Change:** Remove `.setMimeType(MimeTypes.AUDIO_MPEG)` from `playableMediaItem()` so ExoPlayer auto-detects format

### 3. Redundant if/else branching
- **File:** `ui/MusicViewModel.kt:321-326`
- **Change:** Remove the if/else wrapper; keep single `loadHomeContent()` call

### 4. Fade step count asymmetry
- **File:** `playback/MusicPlaybackService.kt:513`
- **Change:** Change `for (i in steps downTo 0)` to `for (i in steps downTo 1)` so fade-in and fade-out both use 20 iterations

### 5-7. Widget and MediaSession branding "S Music" → "High Music"
- **Files:**
  - `playback/MusicPlaybackService.kt:110`
  - `playback/MusicPlaybackService.kt:277`
  - `playback/MusicPlaybackService.kt:286`
  - `playback/PlayerWidgetProvider.kt:19`
- **Change:** Replace all occurrences of `"S Music"` with `"High Music"`

### 8. Empty catch blocks swallow errors silently
- **Files:**
  - `ui/MusicViewModel.kt` (16 instances)
  - `ui/screens/MusicScreens.kt` (3 instances)
  - `playback/MusicPlaybackService.kt` (3 instances)
  - `data/repository/MusicRepository.kt` (1 instance)
- **Change:** Add `import android.util.Log` to each file if missing. Replace each empty `catch (_: Exception) {}` with `catch (e: Exception) { Log.e("MusicApp", "operation failed", e) }` using a descriptive message per catch.

### 9. Duplicate `insertRecentPlay()` calls
- **File:** `playback/PlaybackConnection.kt:106` and `:203`
- **Change:** Remove the call at line 106 (inside `onMediaItemTransition`). Keep the call at line 203 inside `playTrack()` only.

---

## Desktop App (`desktop/`)

### 10. Deprecated Electron `closed` event
- **File:** `desktop/electron/main.js:57`
- **Change:** Replace `win.on('closed', () => { isQuitting = true; app.quit() })` with:
  ```js
  win.on('close', (e) => {
    if (!isQuitting) {
      e.preventDefault()
      isQuitting = true
      app.quit()
    }
  })
  ```

### 11. Custom song ID collision risk
- **File:** `desktop/src/customSongs.ts:33, 55`
- **Change:** Replace `'custom_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6)` with `'custom_' + crypto.randomUUID()` (or `'custom_' + Date.now() + '_' + Math.random().toString(36).slice(2, 10)` if `crypto.randomUUID` is unavailable in target environment)

---

## Validation
- **Android:** Run `./gradlew :app:assembleDebug` to verify build succeeds.
- **Desktop:** Run `npm run lint` and `npm run build` in `desktop/`.
- **Manual:** Verify logout no longer starts playback, FLAC/OGG/M4A files play, widget title shows "High Music".

## Out of Scope
- Spotify playlist import fragility (requires Terms of Service review and structural refactor)
- Desktop Login non-reactive settings (minor UX issue)
- Any feature additions
