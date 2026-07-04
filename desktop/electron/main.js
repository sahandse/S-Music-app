const {
  app,
  BrowserWindow,
  shell,
  Tray,
  Menu,
  nativeImage,
  globalShortcut,
  ipcMain,
  dialog
} = require('electron')
const path = require('path')
const fs = require('fs')
const https = require('https')

const isDev = process.env.ELECTRON_DEV === '1'
const ICON = path.join(__dirname, '..', 'build', 'icon.png')

let win = null
let tray = null
let isQuitting = false
// Remember the normal window bounds so we can restore after mini-mode
let normalBounds = null
let miniMode = false

function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 360,
    minHeight: 120,
    backgroundColor: '#0d0d10',
    title: 'High Music',
    icon: ICON,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      // The renderer talks directly to the user's own Subsonic server, which
      // is often plain http and may not send CORS headers — disabling web
      // security lets those requests and cover/stream URLs work.
      webSecurity: false
    }
  })

  // Open external links (e.g. server web UI) in the system browser
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http')) {
      shell.openExternal(url)
      return { action: 'deny' }
    }
    return { action: 'allow' }
  })

  // Closing the window hides it to the tray instead of quitting
  win.on('close', (e) => {
    if (!isQuitting) {
      e.preventDefault()
      win.hide()
    }
  })

  if (isDev) {
    win.loadURL('http://localhost:5173')
    win.webContents.openDevTools({ mode: 'detach' })
  } else {
    win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))
  }
}

// --- Tray ---
function send(channel, ...args) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, ...args)
}

function buildTray() {
  let img = nativeImage.createFromPath(ICON)
  if (!img.isEmpty()) img = img.resize({ width: 18, height: 18 })
  tray = new Tray(img)
  tray.setToolTip('High Music')
  const menu = Menu.buildFromTemplate([
    { label: 'نمایش برنامه', click: () => showWindow() },
    { type: 'separator' },
    { label: 'پخش/توقف', click: () => send('media-control', 'playpause') },
    { label: 'بعدی', click: () => send('media-control', 'next') },
    { label: 'قبلی', click: () => send('media-control', 'prev') },
    { type: 'separator' },
    {
      label: 'خروج',
      click: () => {
        isQuitting = true
        app.quit()
      }
    }
  ])
  tray.setContextMenu(menu)
  tray.on('click', () => showWindow())
}

function showWindow() {
  if (!win) return
  win.show()
  win.focus()
}

// --- Global media keys ---
function registerMediaKeys() {
  try {
    globalShortcut.register('MediaPlayPause', () => send('media-control', 'playpause'))
    globalShortcut.register('MediaNextTrack', () => send('media-control', 'next'))
    globalShortcut.register('MediaPreviousTrack', () => send('media-control', 'prev'))
    globalShortcut.register('MediaStop', () => send('media-control', 'pause'))
  } catch (_) {
    /* some environments disallow global shortcuts — ignore */
  }
}

// --- Mini-player mode: shrink + always-on-top ---
function setMini(on) {
  if (!win) return
  miniMode = on
  if (on) {
    normalBounds = win.getBounds()
    win.setMinimumSize(340, 96)
    win.setSize(360, 116)
    win.setAlwaysOnTop(true, 'floating')
    win.setResizable(false)
  } else {
    win.setResizable(true)
    win.setAlwaysOnTop(false)
    win.setMinimumSize(360, 120)
    if (normalBounds) win.setBounds(normalBounds)
    else win.setSize(1280, 800)
  }
  send('mini-mode', on)
}

// --- IPC: dialogs, file import, downloads ---
function initIpc() {
  ipcMain.on('window-mini', (_e, on) => setMini(!!on))
  ipcMain.on('window-hide', () => win && win.hide())
  ipcMain.on('open-external', (_e, url) => {
    if (typeof url === 'string' && url.startsWith('http')) shell.openExternal(url)
  })

  // Pick local audio files to add to the queue
  ipcMain.handle('pick-audio', async () => {
    const r = await dialog.showOpenDialog(win, {
      title: 'انتخاب آهنگ‌های محلی',
      properties: ['openFile', 'multiSelections'],
      filters: [{ name: 'Audio', extensions: ['mp3', 'flac', 'wav', 'm4a', 'aac', 'ogg', 'opus', 'wma'] }]
    })
    if (r.canceled) return []
    return r.filePaths.map((p) => ({
      path: p,
      url: 'file://' + p.replace(/\\/g, '/'),
      name: path.basename(p).replace(/\.[^.]+$/, '')
    }))
  })

  // Save arbitrary text (e.g. an .m3u playlist) via a save dialog
  ipcMain.handle('save-text', async (_e, { defaultName, content }) => {
    const r = await dialog.showSaveDialog(win, { defaultPath: defaultName })
    if (r.canceled || !r.filePath) return false
    fs.writeFileSync(r.filePath, content, 'utf8')
    return true
  })

  // Choose a download folder (persisted on the renderer side)
  ipcMain.handle('pick-folder', async () => {
    const r = await dialog.showOpenDialog(win, { properties: ['openDirectory', 'createDirectory'] })
    if (r.canceled) return null
    return r.filePaths[0]
  })

  // Download a URL to a folder; returns the saved path
  ipcMain.handle('download-file', async (_e, { url, folder, filename }) => {
    return await downloadTo(url, folder, filename)
  })
}

function downloadTo(url, folder, filename) {
  return new Promise((resolve, reject) => {
    try {
      if (!folder) return reject(new Error('no folder'))
      fs.mkdirSync(folder, { recursive: true })
      const safe = filename.replace(/[\\/:*?"<>|]/g, '_')
      const dest = path.join(folder, safe)
      const file = fs.createWriteStream(dest)
      const get = (u) => {
        https
          .get(u, (res) => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
              res.destroy()
              return get(res.headers.location)
            }
            if (res.statusCode !== 200) {
              res.destroy()
              return reject(new Error('HTTP ' + res.statusCode))
            }
            res.pipe(file)
            file.on('finish', () => file.close(() => resolve(dest)))
          })
          .on('error', reject)
      }
      get(url)
    } catch (e) {
      reject(e)
    }
  })
}

// Single-instance: focus the existing window instead of a second one
const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => showWindow())

  app.whenReady().then(() => {
    createWindow()
    buildTray()
    registerMediaKeys()
    initIpc()
    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow()
      else showWindow()
    })
  })
}

app.on('before-quit', () => {
  isQuitting = true
})

app.on('will-quit', () => {
  globalShortcut.unregisterAll()
})

// Keep running in the tray when all windows are closed
app.on('window-all-closed', () => {
  // On Windows we stay alive in the tray; only quit when the user asks.
})
