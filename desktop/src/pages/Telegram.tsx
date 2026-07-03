import React, { useRef, useState } from 'react'
import { useApp } from '../store'
import { Cover } from '../components/common'
import {
  connectTelegram,
  loadTgConfig,
  loadTgSongs,
  removeTgSong,
  resolveTgUrl,
  saveTgConfig,
  TgConfig,
  TgSong,
  tgToSong,
  uploadTgSong
} from '../telegram'

// Telegram cloud: store your songs on your own bot (free, unlimited).
export function Telegram() {
  const { playQueue, current } = useApp()
  const [cfg, setCfg] = useState<TgConfig | null>(() => loadTgConfig())
  const [songs, setSongs] = useState<TgSong[]>(() => loadTgSongs())
  const [msg, setMsg] = useState('')

  if (!cfg) {
    return <Connect onDone={(c) => setCfg(c)} />
  }

  return (
    <TelegramLibrary
      cfg={cfg}
      songs={songs}
      setSongs={setSongs}
      msg={msg}
      setMsg={setMsg}
      onDisconnect={() => {
        saveTgConfig(null)
        setCfg(null)
      }}
      onPlay={async (i) => {
        setMsg('در حال دریافت لینک...')
        try {
          const s = songs[i]
          const url = await resolveTgUrl(cfg, s.fileId)
          playQueue([tgToSong(s, url)], 0)
          setMsg('')
        } catch (e: any) {
          setMsg(e?.message || 'خطا در پخش')
        }
      }}
      current={current?.id}
    />
  )
}

function Connect({ onDone }: { onDone: (c: TgConfig) => void }) {
  const [token, setToken] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const connect = async () => {
    setErr('')
    setBusy(true)
    try {
      const c = await connectTelegram(token)
      onDone(c)
    } catch (e: any) {
      setErr(e?.message || 'خطا در اتصال')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ maxWidth: 620 }}>
      <div className="page-title">☁️ فضای ابری تلگرام</div>
      <p className="card-sub" style={{ lineHeight: '22px', marginBottom: 18 }}>
        آهنگ‌های خود را در تلگرام (فضای نامحدود رایگان) ذخیره کنید.
        <br />
        ۱) در تلگرام از <b>@BotFather</b> یک بات بسازید و توکن آن را کپی کنید
        <br />
        ۲) به بات خود پیام <b>/start</b> بدهید
        <br />
        ۳) توکن را اینجا وارد کنید
      </p>
      <div className="field">
        <label>توکن بات</label>
        <input value={token} onChange={(e) => setToken(e.target.value)} dir="ltr" placeholder="123456:ABC-..." />
      </div>
      <button className="btn" onClick={connect} disabled={busy || !token.trim()}>
        {busy ? 'در حال اتصال...' : 'اتصال'}
      </button>
      {err && <div className="login-error" style={{ textAlign: 'right' }}>{err}</div>}
    </div>
  )
}

function TelegramLibrary({
  cfg,
  songs,
  setSongs,
  msg,
  setMsg,
  onDisconnect,
  onPlay,
  current
}: {
  cfg: TgConfig
  songs: TgSong[]
  setSongs: (s: TgSong[]) => void
  msg: string
  setMsg: (s: string) => void
  onDisconnect: () => void
  onPlay: (i: number) => void
  current?: string
}) {
  const fileRef = useRef<HTMLInputElement | null>(null)
  const [pending, setPending] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [artist, setArtist] = useState('')
  const [cover, setCover] = useState('')
  const [uploading, setUploading] = useState(false)

  const doUpload = async () => {
    if (!pending) return
    setUploading(true)
    setMsg('در حال آپلود...')
    try {
      const next = await uploadTgSong(cfg, pending, title, artist, cover)
      setSongs(next)
      setPending(null)
      setTitle('')
      setArtist('')
      setCover('')
      setMsg('✓ آپلود شد')
    } catch (e: any) {
      setMsg('خطا در آپلود: ' + (e?.message || ''))
    } finally {
      setUploading(false)
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="page-title" style={{ marginBottom: 0 }}>
          ☁️ فضای ابری تلگرام
        </div>
        <button className="chip" onClick={onDisconnect}>
          قطع اتصال
        </button>
      </div>
      <p className="card-sub" style={{ margin: '8px 0 18px' }}>
        {songs.length} آهنگ ذخیره‌شده · فایل تا ۲۰ مگابایت قابل پخش است
      </p>

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
      <button className="btn" onClick={() => fileRef.current?.click()} disabled={uploading}>
        + آپلود آهنگ جدید
      </button>
      {msg && <div style={{ color: 'var(--accent)', fontSize: 13, marginTop: 10 }}>{msg}</div>}

      {pending && (
        <div
          style={{
            background: 'var(--bg-elevated)',
            borderRadius: 12,
            padding: 16,
            marginTop: 16,
            maxWidth: 560
          }}
        >
          <div className="card-sub" style={{ marginBottom: 10 }} dir="ltr">
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
          <div className="row-actions">
            <button className="btn" onClick={doUpload} disabled={uploading}>
              {uploading ? 'در حال آپلود...' : 'آپلود'}
            </button>
            <button className="btn ghost" onClick={() => setPending(null)} disabled={uploading}>
              انصراف
            </button>
          </div>
        </div>
      )}

      <div className="section-title">لیست ({songs.length})</div>
      {songs.length === 0 ? (
        <div className="empty">هنوز آهنگی آپلود نشده است</div>
      ) : (
        <div>
          {songs.map((s, i) => (
            <div
              key={s.fileId}
              className={`track-row ${current === 'tg_' + s.fileId ? 'playing' : ''}`}
              style={{ gridTemplateColumns: '48px 1fr 90px 40px' }}
            >
              <div
                onClick={() => onPlay(i)}
                style={{ cursor: 'pointer', width: 40, height: 40, borderRadius: 6, overflow: 'hidden' }}
              >
                <Cover directUrl={s.coverUrl} size={80} className="my-song-cover" />
              </div>
              <div className="t-title" onClick={() => onPlay(i)} style={{ cursor: 'pointer' }}>
                {s.title}
                <div className="card-sub" style={{ marginTop: 2 }}>
                  {s.artist}
                </div>
              </div>
              <div className="t-dur">{s.sizeBytes ? (s.sizeBytes / 1048576).toFixed(1) + ' MB' : ''}</div>
              <div className="t-star" style={{ opacity: 1 }} title="حذف" onClick={() => setSongs(removeTgSong(s.fileId))}>
                🗑
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
