import React, { useState } from 'react'
import { useApp } from '../store'
import { makeCreds, ping } from '../api'
import { t } from '../i18n'
import { getSettings, setSettings } from '../settings'

// The server is fixed; users only enter their username + password.
const SERVER_URL = 'https://soundtron.ir'
// Where new users register (change to your own Telegram id/handle)
const SIGNUP_TELEGRAM = 'https://t.me/himusicapp'

export function Login() {
  const { setCreds } = useApp()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [, force] = useState(0)
  const lang = getSettings().lang

  const toggleLang = () => {
    setSettings({ lang: lang === 'fa' ? 'en' : 'fa' })
    force((n) => n + 1)
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!username) {
      setError(t('login.needUsername'))
      return
    }
    setBusy(true)
    const creds = makeCreds(SERVER_URL, username, password)
    try {
      await ping(creds)
      setCreds(creds)
    } catch (err: any) {
      setError(t('login.connectFail') + (err?.message || t('login.wrongCreds')))
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <button className="lang-toggle" onClick={toggleLang} type="button">
        {lang === 'fa' ? 'English' : 'فارسی'}
      </button>
      <form className="login-card" onSubmit={submit}>
        <h1>
          High<span className="dot">Music</span>
        </h1>
        <p>{t('login.subtitle')}</p>

        <div className="field">
          <label>{t('login.username')}</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} dir="ltr" autoFocus />
        </div>
        <div className="field">
          <label>{t('login.password')}</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            dir="ltr"
          />
        </div>

        <button className="btn" type="submit" disabled={busy}>
          {busy ? t('login.connecting') : t('login.connect')}
        </button>
        {error && <div className="login-error">{error}</div>}

        <div style={{ textAlign: 'center', marginTop: 18, fontSize: 13, color: 'var(--text-dim)' }}>
          {t('login.noAccount')}{' '}
          <a href={SIGNUP_TELEGRAM} style={{ color: 'var(--accent)', fontWeight: 700 }}>
            {t('login.signup')}
          </a>
        </div>
      </form>
    </div>
  )
}
