import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Cover } from '../components/common'
import { catalogToSong, CatalogSong, fetchCatalog } from '../catalog'

// Shared online catalog: read a public catalog.json and play direct URLs.
export function Catalog() {
  const { playQueue, current } = useApp()
  const [songs, setSongs] = useState<CatalogSong[]>([])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const reload = async () => {
    setBusy(true)
    setErr('')
    try {
      setSongs(await fetchCatalog())
    } catch (e: any) {
      setErr(e?.message || 'خطا در دریافت کاتالوگ')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    reload()
  }, [])

  const playAll = (startIndex: number) => {
    playQueue(songs.map(catalogToSong), startIndex)
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="page-title" style={{ marginBottom: 0 }}>
          🎵 کاتالوگ آنلاین
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {songs.length > 0 && (
            <button className="chip active" onClick={() => playAll(0)}>
              ▶ پخش همه
            </button>
          )}
          <button className="chip" onClick={reload}>
            بروزرسانی
          </button>
        </div>
      </div>
      <p className="card-sub" style={{ margin: '8px 0 16px' }}>
        {songs.length} آهنگ · آهنگ‌های اشتراکی که ادمین در کانال منتشر می‌کند
      </p>

      {busy && songs.length === 0 ? (
        <div className="empty">در حال بارگذاری...</div>
      ) : err ? (
        <div className="empty">{err}</div>
      ) : songs.length === 0 ? (
        <div className="empty">کاتالوگ هنوز خالی است</div>
      ) : (
        <div>
          {songs.map((s, i) => (
            <div
              key={s.audioUrl}
              className={`track-row ${current === 'cat_' + s.audioUrl ? 'playing' : ''}`}
              style={{ gridTemplateColumns: '48px 1fr 40px' }}
            >
              <div
                onClick={() => playAll(i)}
                style={{ cursor: 'pointer', width: 40, height: 40, borderRadius: 6, overflow: 'hidden' }}
              >
                <Cover directUrl={s.coverUrl} size={80} className="my-song-cover" />
              </div>
              <div className="t-title" onClick={() => playAll(i)} style={{ cursor: 'pointer' }}>
                {s.title}
                <div className="card-sub" style={{ marginTop: 2 }}>
                  {s.artist}
                </div>
              </div>
              <div className="t-index" onClick={() => playAll(i)} style={{ cursor: 'pointer' }}>
                ▶
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
