# High Music — Desktop (Windows)

A desktop client for your Subsonic / Navidrome server, styled after
[Feishin](https://github.com/jeffvli/feishin). It connects to the **same server**
as the Android app using the same token auth, so all your music is already there.

## Features

- Connect to any Subsonic/Navidrome server (URL + username + password)
- Home (newest / most played / random), Albums (sortable), Artists, Playlists,
  Starred, Search
- Album / artist / playlist detail pages with track lists
- Full player bar: play/pause, prev/next, seek, volume, shuffle, repeat, queue
- **My Songs** — add your own tracks by pasting a cover URL, artist, title and a
  **direct audio link**; stored locally and played through the same player. Bulk
  import via a pasted JSON array is supported.

## Develop

```bash
cd desktop
npm install
npm run dev      # Vite dev server + Electron window
```

## Build the Windows installer

```bash
npm run build    # vite build + electron-builder --win  → desktop/release/*.exe
```

CI (`.github/workflows/build-windows.yml`) builds the installer on every push
that touches `desktop/` and publishes it to the `latest-windows-build` release.

## Notes

- `webSecurity` is disabled in the Electron window so requests to a plain-`http`
  server (and its cover/stream URLs) work without CORS headers.
- Credentials are stored locally with `localStorage`; the password is never
  saved — only the derived Subsonic token + salt.
