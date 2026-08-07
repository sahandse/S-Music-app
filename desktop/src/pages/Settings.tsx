import React from 'react'
import { useApp } from '../store'
import { setSettings, EQ_FREQS, ThemeMode, Lang, exportSettings, importSettings } from '../settings'
import { bridge, isDesktop } from '../desktop'
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

const RELEASES_URL = 'https://github.com/sahandse/s-music-app/releases/tag/latest-windows-build'

export function Settings() {
  const { settings } = useApp()
  const s = settings

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

  const openReleases = () => {
    const b = bridge()
    if (b) b.openExternal(RELEASES_URL)
    else window.open(RELEASES_URL, '_blank')
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

      {/* Version / releases */}
      {isDesktop() && (
        <section className="settings-card">
          <h3>{t('set.updates')}</h3>
          <div className="set-row">
            <span>
              {t('set.currentVersion')}
              {bridge()?.version || '—'}
            </span>
            <button className="btn-sm" onClick={openReleases}>
              {t('set.openReleases')}
            </button>
          </div>
          <p className="card-sub" style={{ marginTop: 4 }}>
            {t('set.releasesHint')}
          </p>
        </section>
      )}

      {/* Backup / restore */}
      {isDesktop() && (
        <section className="settings-card">
          <h3>{t('set.backup') || 'پشتیبان‌گیری'}</h3>
          <div className="set-row">
            <span>{t('set.exportSettings') || 'Export Settings'}</span>
            <button className="btn-sm" onClick={() => {
              const json = exportSettings()
              const blob = new Blob([json], { type: 'application/json' })
              const url = URL.createObjectURL(blob)
              const a = document.createElement('a')
              a.href = url
              a.download = 'high-music-settings.json'
              a.click()
              URL.revokeObjectURL(url)
            }}>
              {t('set.export') || 'Export'}
            </button>
          </div>
          <div className="set-row">
            <span>{t('set.importSettings') || 'Import Settings'}</span>
            <label className="btn-sm" style={{ cursor: 'pointer' }}>
              {t('set.import') || 'Import'}
              <input
                type="file"
                accept="application/json"
                style={{ display: 'none' }}
                onChange={(e) => {
                  const file = e.target.files?.[0]
                  if (!file) return
                  const reader = new FileReader()
                  reader.onload = () => {
                    const ok = importSettings(reader.result as string)
                    if (!ok) alert('Invalid settings file')
                  }
                  reader.readAsText(file)
                }}
              />
            </label>
          </div>
        </section>
      )}
    </div>
  )
}
