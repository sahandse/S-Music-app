const { contextBridge, ipcRenderer } = require('electron')

// Safe bridge between the renderer (React app) and the main process. Exposes a
// small, explicit surface: media-key/tray control events, native dialogs for
// local files and downloads, mini-player mode, and the auto-updater.
contextBridge.exposeInMainWorld('desktop', {
  platform: process.platform,
  version: '1.2.0',

  // main -> renderer: media key / tray commands ("playpause" | "next" | "prev" | "pause")
  onMediaControl: (cb) => {
    const h = (_e, action) => cb(action)
    ipcRenderer.on('media-control', h)
    return () => ipcRenderer.removeListener('media-control', h)
  },
  onMiniMode: (cb) => {
    const h = (_e, on) => cb(on)
    ipcRenderer.on('mini-mode', h)
    return () => ipcRenderer.removeListener('mini-mode', h)
  },
  onUpdateStatus: (cb) => {
    const h = (_e, s) => cb(s)
    ipcRenderer.on('update-status', h)
    return () => ipcRenderer.removeListener('update-status', h)
  },

  // window / mini
  setMini: (on) => ipcRenderer.send('window-mini', on),
  hideWindow: () => ipcRenderer.send('window-hide'),

  // dialogs & files
  pickAudio: () => ipcRenderer.invoke('pick-audio'),
  saveText: (defaultName, content) => ipcRenderer.invoke('save-text', { defaultName, content }),
  pickFolder: () => ipcRenderer.invoke('pick-folder'),
  downloadFile: (url, folder, filename) => ipcRenderer.invoke('download-file', { url, folder, filename }),

  // updates
  checkUpdate: () => ipcRenderer.invoke('update-check'),
  downloadUpdate: () => ipcRenderer.invoke('update-download'),
  installUpdate: () => ipcRenderer.invoke('update-install')
})
