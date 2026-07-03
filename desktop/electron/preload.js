const { contextBridge } = require('electron')

// Minimal, safe bridge. The renderer is a self-contained Subsonic client, so
// there's nothing privileged to expose yet — this keeps contextIsolation on
// while leaving room for future native integrations (media keys, tray, ...).
contextBridge.exposeInMainWorld('desktop', {
  platform: process.platform,
  version: '1.0.0'
})
