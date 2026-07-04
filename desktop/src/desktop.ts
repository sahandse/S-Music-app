// Typed accessor for the Electron preload bridge (window.desktop). When the app
// runs in a plain browser (e.g. `vite` dev without Electron) the bridge is
// undefined, so every helper degrades gracefully.

export interface LocalFile {
  path: string
  url: string
  name: string
}

interface DesktopBridge {
  platform: string
  version: string
  onMediaControl: (cb: (action: string) => void) => () => void
  onMiniMode: (cb: (on: boolean) => void) => () => void
  setMini: (on: boolean) => void
  hideWindow: () => void
  openExternal: (url: string) => void
  pickAudio: () => Promise<LocalFile[]>
  saveText: (defaultName: string, content: string) => Promise<boolean>
  pickFolder: () => Promise<string | null>
  downloadFile: (url: string, folder: string, filename: string) => Promise<string>
}

declare global {
  interface Window {
    desktop?: DesktopBridge
  }
}

export function bridge(): DesktopBridge | undefined {
  return typeof window !== 'undefined' ? window.desktop : undefined
}

export function isDesktop(): boolean {
  return !!bridge()
}
