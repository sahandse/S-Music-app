import React, { useState } from 'react'
import { useApp } from '../store'
import { setSettings, EQ_FREQS, ThemeMode, Lang } from '../settings'
import { bridge, isDesktop, UpdateStatus } from '../desktop'
import { lastfmLogin, lastfmLogout } from '../lastfm'
import { t } from '../i18n'

const ACCENTS = ['#e0264e', '#7c5cff', '#1db954', '#ff7a00', '#00b4d8', '#ff3b7f']
const EQ_LABELS = ['60', '230', '910', '3.6k', '14k']
const EQ_PRESETS: Array<{ key: string; bands: number[] }> = [
  { key: 'eq.neutral', bands: [0, 0, 0, 0, 0] },
  { key: 'eq.bass', bands: [6, 4, 0, 0, 2] },
  { key: 'eq.pop', bands: [-1, 2, 4, 2, -1] },
  { key: 'eq.rock', bands: [4, 2, -1, 2, 4] },
  { key: 'eq.clear', bands: [-2, 0, 2, 4, 3] }
]

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
      setLfmMsg(t('lfm.connected') + name)
      setLfmPass('')
    } catch (e: any) {
      setLfmMsg(e?.message || t('lfm.error'))
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
      <h1>{t('set.title')}</h1>

      {/* Appearance + language */}
      <section className="settings-card">
        <h3>{t('set.appearance')}</h3>
        <div className="set-row">
          <span>{t('set.language')}</span>
          <div className="seg">
            {(['fa', 'en'] as Lang[]).map((l) => (
              <button key={l} className={s.lang === l ? 'on' : ''} onClick={() => setSettings({ lang: l })}>
                {l === 'fa' ? 'فارسی' : 'English'}
              </button>
            ))}
          </div>
        </div>
        <div className="set-row">
          <span>{t('set.theme')}</span>
          <div className="seg">
            {(['dark', 'light'] as ThemeMode[]).map((th) => (
              <button key={th} className={s.theme === th ? 'on' : ''} onClick={() => setSettings({ theme: th })}>
                {th === 'dark' ? t('set.dark') : t('set.light')}
              </button>
            ))}
          </div>
        </div>
        <div className="set-row">
          <span>{t('set.accent')}</span>
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
              title={t('set.customColor')}
            />
          </div>
        </div>
      </section>

      {/* Playback */}
      <section className="settings-card">
        <h3>{t('set.playback')}</h3>
        <label className="set-row toggle">
          <span>{t('set.fadeOnPause')}</span>
          <input
            type="checkbox"
            checked={s.fadeOnPause}
            onChange={(e) => setSettings({ fadeOnPause: e.target.checked })}
          />
        </label>
        <div className="set-row">
          <span>
            {t('set.crossfade')}: {s.crossfadeSec}s
          </span>
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
        <h3>{t('set.eq')}</h3>
        <label className="set-row toggle">
          <span>{t('set.enabled')}</span>
          <input
            type="checkbox"
            checked={s.eqEnabled}
            onChange={(e) => setSettings({ eqEnabled: e.target.checked })}
          />
        </label>
        <div className="eq-presets">
          {EQ_PRESETS.map((p) => (
            <button key={p.key} onClick={() => setSettings({ eqBands: p.bands, eqEnabled: true })}>
              {t(p.key)}
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
          <h3>{t('set.download')}</h3>
          <div className="set-row">
            <span style={{ direction: 'ltr', fontSize: 12, color: 'var(--text-dim)' }}>
              {s.downloadFolder || t('set.noFolder')}
            </span>
            <button className="btn-sm" onClick={pickFolder}>
              {t('set.pickFolder')}
            </button>
          </div>
        </section>
      )}

      {/* Last.fm */}
      <section className="settings-card">
        <h3>{t('set.lastfm')}</h3>
        {s.lastfmEnabled ? (
          <div className="set-row">
            <span>
              {t('set.connectedTo')}
              {s.lastfmUser}
            </span>
            <button className="btn-sm" onClick={lastfmLogout}>
              {t('set.disconnect')}
            </button>
          </div>
        ) : (
          <>
            <p className="card-sub" style={{ marginBottom: 8 }}>
              {t('set.lastfmDesc')}
            </p>
            <input
              className="inp"
              placeholder={t('set.apiKey')}
              dir="ltr"
              value={s.lastfmKey}
              onChange={(e) => setSettings({ lastfmKey: e.target.value })}
            />
            <input
              className="inp"
              placeholder={t('set.apiSecret')}
              dir="ltr"
              value={s.lastfmSecret}
              onChange={(e) => setSettings({ lastfmSecret: e.target.value })}
            />
            <input
              className="inp"
              placeholder={t('set.lastfmUser')}
              dir="ltr"
              value={lfmUser}
              onChange={(e) => setLfmUser(e.target.value)}
            />
            <input
              className="inp"
              type="password"
              placeholder={t('set.lastfmPass')}
              dir="ltr"
              value={lfmPass}
              onChange={(e) => setLfmPass(e.target.value)}
            />
            <button className="btn-sm" onClick={doLastfmLogin}>
              {t('set.connect')}
            </button>
          </>
        )}
        {lfmMsg && <div className="card-sub" style={{ marginTop: 8 }}>{lfmMsg}</div>}
      </section>

      {/* Updates */}
      {isDesktop() && (
        <section className="settings-card">
          <h3>{t('set.updates')}</h3>
          <div className="set-row">
            <span>
              {t('set.currentVersion')}
              {bridge()?.version || '—'}
              {upd?.state === 'available' && t('set.updateAvailable', { v: upd.version || '' })}
              {upd?.state === 'none' && t('set.upToDate')}
              {upd?.state === 'downloading' && t('set.downloading', { p: upd.percent ?? 0 })}
              {upd?.state === 'ready' && t('set.readyInstall')}
              {upd?.state === 'error' && t('set.checkFailed')}
            </span>
            {upd?.state === 'available' ? (
              <button className="btn-sm" onClick={() => bridge()?.downloadUpdate()}>
                {t('set.downloadBtn')}
              </button>
            ) : upd?.state === 'ready' ? (
              <button className="btn-sm" onClick={() => bridge()?.installUpdate()}>
                {t('set.installBtn')}
              </button>
            ) : (
              <button className="btn-sm" onClick={checkUpdate}>
                {t('set.checkBtn')}
              </button>
            )}
          </div>
        </section>
      )}
    </div>
  )
}
