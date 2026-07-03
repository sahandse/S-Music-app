import React, { useEffect, useRef, useState } from 'react'
import { useApp } from '../store'
import { Cover } from '../components/common'
import {
  adminAddSong,
  catalogToSong,
  CatalogConfig,
  CatalogSong,
  configFromUrl,
  fetchCatalog,
  loadCatalogConfig,
  resolveCatalogUrl,
  saveCatalogConfig
} from '../catalog'

// Shared online catalog curated by an admin via a Telegram channel.
export function Catalog() {
  const { playQueue, current } = useApp()
  const [cfg, setCfg] = useState<CatalogConfig | null>(() => loadCatalogConfig())
  const [songs, setSongs] = useState<CatalogSong[]>([])
  const [msg, setMsg] = useState('')
  const [busy, setBusy] = useState(false)
  const [adminMode, setAdminMode] = useState(false)

  const reload = async (c: CatalogConfig) => {
    setBusy(true)
    try {
      setSongs(await fetchCatalog(c))
      setMsg('')
    } catch (e: any) {
      setMsg(e?.message || 'خطا در دریافت کاتالوگ')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    if (cfg) reload(cfg)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cfg])

  if (!cfg) return <ConnectCatalog onDone={setCfg} />

  const play = async (i: number) => {
    setMsg('در حال دریافت لینک...')
    try {
      const url = await resolveCatalogUrl(cfg, songs[i].fileId)
      playQueue([catalogToSong(songs[i], url)], 0)
      setMsg('')
    } catch (e: any) {
      setMsg(e?.message || 'خطا در پخش')
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="page-title" style={{ marginBottom: 0 }}>
          🎵 کاتالوگ آنلاین
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="chip" onClick={() => reload(cfg)}>
            بروزرسانی
          </button>
          <button className={`chip ${adminMode ? 'active' : ''}`} onClick={() => setAdminMode((v) => !v)}>
            {adminMode ? 'حالت کاربر' : 'حالت ادمین'}
          </button>
          <button
            className="chip"
            onClick={() => {
              saveCatalogConfig(null)
              setCfg(null)
              setSongs([])
            }}
          >
            قطع اتصال
          </button>
        </div>
      </div>
      <p className="card-sub" style={{ margin: '8px 0 16px' }}>
        {songs.length} آهنگ در کاتالوگ · فایل تا ۲۰ مگابایت قابل پخش است
      </p>

      {adminMode && (
        <AdminUpload
          cfg={cfg}
          songs={songs}
          setSongs={setSongs}
          busy={busy}
          setBusy={setBusy}
          setMsg={setMsg}
        />
      )}
      {busy && <div className="empty">در حال کار...</div>}
      {msg && <div style={{ color: 'var(--accent)', fontSize: 13, margin: '8px 0' }}>{msg}</div>}

      <div className="section-title">لیست</div>
      {songs.length === 0 ? (
        <div className="empty">کاتالوگ خالی است</div>
      ) : (
        <div>
          {songs.map((s, i) => (
            <div
              key={s.fileId}
              className={`track-row ${current === 'tg_' + s.fileId ? 'playing' : ''}`}
              style={{ gridTemplateColumns: '48px 1fr 40px' }}
            >
              <div
                onClick={() => play(i)}
                style={{ cursor: 'pointer', width: 40, height: 40, borderRadius: 6, overflow: 'hidden' }}
              >
                <Cover directUrl={s.coverUrl} size={80} className="my-song-cover" />
              </div>
              <div className="t-title" onClick={() => play(i)} style={{ cursor: 'pointer' }}>
                {s.title}
                <div className="card-sub" style={{ marginTop: 2 }}>
                  {s.artist}
                </div>
              </div>
              <div className="t-index" onClick={() => play(i)} style={{ cursor: 'pointer' }}>
                ▶
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function ConnectCatalog({ onDone }: { onDone: (c: CatalogConfig) => void }) {
  const [url, setUrl] = useState('')
  const [token, setToken] = useState('')
  const [channel, setChannel] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const viaUrl = async () => {
    setErr('')
    setBusy(true)
    try {
      onDone(await configFromUrl(url))
    } catch (e: any) {
      setErr(e?.message || 'خطا')
    } finally {
      setBusy(false)
    }
  }

  const viaAdmin = () => {
    if (!token.trim() || !channel.trim()) return
    const cfg = { token: token.trim(), channel: channel.trim() }
    saveCatalogConfig(cfg)
    onDone(cfg)
  }

  return (
    <div style={{ maxWidth: 560 }}>
      <div className="page-title">🎵 کاتالوگ آنلاین</div>
      <p className="card-sub" style={{ lineHeight: '22px', marginBottom: 16 }}>
        آهنگ‌های مشترک از یک کانال تلگرام. کاربر عادی: آدرس پیکربندی را از ادمین بگیرید. ادمین:
        توکن بات و آیدی کانال را وارد کنید.
      </p>
      <div className="field">
        <label>آدرس پیکربندی (کاربر عادی)</label>
        <input value={url} onChange={(e) => setUrl(e.target.value)} dir="ltr" placeholder="https://…/config.json" />
      </div>
      <button className="btn" onClick={viaUrl} disabled={busy || !url.trim()}>
        اتصال با آدرس
      </button>

      <div style={{ borderTop: '1px solid var(--border)', margin: '22px 0 16px' }} />
      <div className="card-sub" style={{ marginBottom: 10 }}>
        — یا به‌عنوان ادمین —
      </div>
      <div className="field">
        <label>توکن بات</label>
        <input value={token} onChange={(e) => setToken(e.target.value)} dir="ltr" />
      </div>
      <div className="field">
        <label>آیدی کانال (مثل @mychannel)</label>
        <input value={channel} onChange={(e) => setChannel(e.target.value)} dir="ltr" />
      </div>
      <button className="btn" onClick={viaAdmin} disabled={!token.trim() || !channel.trim()}>
        اتصال
      </button>
      {err && <div className="login-error" style={{ textAlign: 'right' }}>{err}</div>}
    </div>
  )
}

function AdminUpload({
  cfg,
  songs,
  setSongs,
  busy,
  setBusy,
  setMsg
}: {
  cfg: CatalogConfig
  songs: CatalogSong[]
  setSongs: (s: CatalogSong[]) => void
  busy: boolean
  setBusy: (b: boolean) => void
  setMsg: (s: string) => void
}) {
  const fileRef = useRef<HTMLInputElement | null>(null)
  const [pending, setPending] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [artist, setArtist] = useState('')
  const [cover, setCover] = useState('')

  const upload = async () => {
    if (!pending) return
    setBusy(true)
    setMsg('در حال آپلود به کاتالوگ...')
    try {
      const next = await adminAddSong(cfg, songs, pending, title, artist, cover)
      setSongs(next)
      setPending(null)
      setTitle('')
      setArtist('')
      setCover('')
      setMsg('✓ به کاتالوگ اضافه شد')
    } catch (e: any) {
      setMsg('خطا: ' + (e?.message || ''))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ background: 'var(--bg-elevated)', borderRadius: 12, padding: 16, marginBottom: 16, maxWidth: 560 }}>
      <input
        ref={fileRef}
        type="file"
        accept="audio/*"
        style={{ display: 'none' }}
        onChange={(e) => {
          const f = e.target.files?.[0]
          if (f) {
            setPending(f)
            setTitle(f.name.replace(/\.[^.]+$/, ''))
          }
        }}
      />
      <button className="btn" onClick={() => fileRef.current?.click()} disabled={busy}>
        + انتخاب فایل آهنگ
      </button>
      {pending && (
        <div style={{ marginTop: 12 }}>
          <div className="card-sub" dir="ltr" style={{ marginBottom: 8 }}>
            {pending.name} · {(pending.size / 1048576).toFixed(1)} MB
          </div>
          <div className="field">
            <label>نام آهنگ</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="field">
            <label>نام هنرمند</label>
            <input value={artist} onChange={(e) => setArtist(e.target.value)} />
          </div>
          <div className="field">
            <label>لینک کاور (اختیاری)</label>
            <input value={cover} onChange={(e) => setCover(e.target.value)} dir="ltr" />
          </div>
          <button className="btn" onClick={upload} disabled={busy}>
            افزودن به کاتالوگ
          </button>
        </div>
      )}
    </div>
  )
}
