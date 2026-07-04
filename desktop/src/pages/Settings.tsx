import React, { useState } from 'react'
import { useApp } from '../store'
import { getSettings, setSettings, EQ_FREQS, ThemeMode } from '../settings'
import { bridge, isDesktop, UpdateStatus } from '../desktop'
import { lastfmLogin, lastfmLogout } from '../lastfm'

const ACCENTS = ['#e0264e', '#7c5cff', '#1db954', '#ff7a00', '#00b4d8', '#ff3b7f']
const EQ_LABELS = ['60', '230', '910', '3.6k', '14k']
const EQ_PRESETS: Record<string, number[]> = {
  'خنثی': [0, 0, 0, 0, 0],
  'بیس‌دار': [6, 4, 0, 0, 2],
  'پاپ': [-1, 2, 4, 2, -1],
  'راک': [4, 2, -1, 2, 4],
  'صدای واضح': [-2, 0, 2, 4, 3]
}

export function Settings() {
  const { settings } = useApp()
  const s = settings

  const [lfmUser, setLfmUser] = useState('')
  const [lfmPass, setLfmPass] = useState('')
  const [lfmMsg, setLfmMsg] = useState('')
  const [upd, setUpd] = useState<UpdateStatus | null>(null)

  React.useEffect(() => {
    const b = bridge()
    if (!b) return
    return b.onUpdateStatus(setUpd)
  }, [])

  const setEqBand = (i: number, v: number) => {
    const bands = s.eqBands.slice()
    bands[i] = v
    setSettings({ eqBands: bands })
  }

  const pickFolder = async () => {
    const b = bridge()
    if (!b) return
    const f = await b.pickFolder()
    if (f) setSettings({ downloadFolder: f })
  }

  const doLastfmLogin = async () => {
    setLfmMsg('')
    try {
      const name = await lastfmLogin(lfmUser, lfmPass)
      setLfmMsg('متصل شد: ' + name)
      setLfmPass('')
    } catch (e: any) {
      setLfmMsg(e?.message || 'خطا')
    }
  }

  const checkUpdate = async () => {
    const b = bridge()
    if (!b) return
    setUpd({ state: 'downloading', percent: 0 })
    const ok = await b.checkUpdate()
    if (!ok) setUpd({ state: 'error' })
  }

  return (
    <div className="page settings-page">
      <h1>تنظیمات</h1>

      {/* Appearance */}
      <section className="settings-card">
        <h3>ظاهر</h3>
        <div className="set-row">
          <span>پوسته</span>
          <div className="seg">
            {(['dark', 'light'] as ThemeMode[]).map((t) => (
              <button
                key={t}
                className={s.theme === t ? 'on' : ''}
                onClick={() => setSettings({ theme: t })}
              >
                {t === 'dark' ? 'تیره' : 'روشن'}
              </button>
            ))}
          </div>
        </div>
        <div className="set-row">
          <span>رنگ لهجه</span>
          <div className="swatches">
            {ACCENTS.map((c) => (
              <button
                key={c}
                className={`swatch ${s.accent === c ? 'on' : ''}`}
                style={{ background: c }}
                onClick={() => setSettings({ accent: c })}
              />
            ))}
            <input
              type="color"
              value={s.accent}
              onChange={(e) => setSettings({ accent: e.target.value })}
              title="رنگ دلخواه"
            />
          </div>
        </div>
      </section>

      {/* Playback */}
      <section className="settings-card">
        <h3>پخش</h3>
        <label className="set-row toggle">
          <span>محو تدریجی هنگام توقف/پخش</span>
          <input
            type="checkbox"
            checked={s.fadeOnPause}
            onChange={(e) => setSettings({ fadeOnPause: e.target.checked })}
          />
        </label>
        <div className="set-row">
          <span>محو بین آهنگ‌ها (Crossfade): {s.crossfadeSec}s</span>
          <input
            type="range"
            min={0}
            max={12}
            step={1}
            value={s.crossfadeSec}
            onChange={(e) => setSettings({ crossfadeSec: parseInt(e.target.value) })}
          />
        </div>
      </section>

      {/* Equalizer */}
      <section className="settings-card">
        <h3>اکولایزر</h3>
        <label className="set-row toggle">
          <span>فعال</span>
          <input
            type="checkbox"
            checked={s.eqEnabled}
            onChange={(e) => setSettings({ eqEnabled: e.target.checked })}
          />
        </label>
        <div className="eq-presets">
          {Object.entries(EQ_PRESETS).map(([name, bands]) => (
            <button key={name} onClick={() => setSettings({ eqBands: bands, eqEnabled: true })}>
              {name}
            </button>
          ))}
        </div>
        <div className={`eq-bands ${s.eqEnabled ? '' : 'disabled'}`}>
          {EQ_FREQS.map((_, i) => (
            <div key={i} className="eq-band">
              <input
                type="range"
                min={-12}
                max={12}
                step={1}
                value={s.eqBands[i]}
                onChange={(e) => setEqBand(i, parseInt(e.target.value))}
                // vertical slider
                style={{ writingMode: 'vertical-lr' as any, direction: 'rtl' }}
              />
              <span className="eq-val">{s.eqBands[i] > 0 ? '+' + s.eqBands[i] : s.eqBands[i]}</span>
              <span className="eq-label">{EQ_LABELS[i]}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Downloads */}
      {isDesktop() && (
        <section className="settings-card">
          <h3>دانلود</h3>
          <div className="set-row">
            <span style={{ direction: 'ltr', fontSize: 12, color: 'var(--text-dim)' }}>
              {s.downloadFolder || 'پوشه‌ای انتخاب نشده'}
            </span>
            <button className="btn-sm" onClick={pickFolder}>
              انتخاب پوشه
            </button>
          </div>
        </section>
      )}

      {/* Last.fm */}
      <section className="settings-card">
        <h3>Last.fm (اسکروبل)</h3>
        {s.lastfmEnabled ? (
          <div className="set-row">
            <span>متصل به: {s.lastfmUser}</span>
            <button className="btn-sm" onClick={lastfmLogout}>
              قطع اتصال
            </button>
          </div>
        ) : (
          <>
            <p className="card-sub" style={{ marginBottom: 8 }}>
              کلید API و Secret را از last.fm/api بگیرید، سپس با حساب Last.fm خود وارد شوید.
            </p>
            <input
              className="inp"
              placeholder="API Key"
              dir="ltr"
              value={s.lastfmKey}
              onChange={(e) => setSettings({ lastfmKey: e.target.value })}
            />
            <input
              className="inp"
              placeholder="API Secret"
              dir="ltr"
              value={s.lastfmSecret}
              onChange={(e) => setSettings({ lastfmSecret: e.target.value })}
            />
            <input
              className="inp"
              placeholder="نام کاربری Last.fm"
              dir="ltr"
              value={lfmUser}
              onChange={(e) => setLfmUser(e.target.value)}
            />
            <input
              className="inp"
              type="password"
              placeholder="رمز Last.fm"
              dir="ltr"
              value={lfmPass}
              onChange={(e) => setLfmPass(e.target.value)}
            />
            <button className="btn-sm" onClick={doLastfmLogin}>
              اتصال
            </button>
          </>
        )}
        {lfmMsg && <div className="card-sub" style={{ marginTop: 8 }}>{lfmMsg}</div>}
      </section>

      {/* Updates */}
      {isDesktop() && (
        <section className="settings-card">
          <h3>به‌روزرسانی</h3>
          <div className="set-row">
            <span>
              نسخه فعلی: {bridge()?.version || '—'}
              {upd?.state === 'available' && ` — نسخه ${upd.version} موجود است`}
              {upd?.state === 'none' && ' — به‌روز هستید'}
              {upd?.state === 'downloading' && ` — دانلود ${upd.percent ?? 0}%`}
              {upd?.state === 'ready' && ' — آماده نصب'}
              {upd?.state === 'error' && ' — بررسی ناموفق'}
            </span>
            {upd?.state === 'available' ? (
              <button className="btn-sm" onClick={() => bridge()?.downloadUpdate()}>
                دانلود
              </button>
            ) : upd?.state === 'ready' ? (
              <button className="btn-sm" onClick={() => bridge()?.installUpdate()}>
                نصب و راه‌اندازی مجدد
              </button>
            ) : (
              <button className="btn-sm" onClick={checkUpdate}>
                بررسی به‌روزرسانی
              </button>
            )}
          </div>
        </section>
      )}
    </div>
  )
}
