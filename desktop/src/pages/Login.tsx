import React, { useState } from 'react'
import { useApp } from '../store'
import { makeCreds, ping } from '../api'

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

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!username) {
      setError('نام کاربری لازم است')
      return
    }
    setBusy(true)
    const creds = makeCreds(SERVER_URL, username, password)
    try {
      await ping(creds)
      setCreds(creds)
    } catch (err: any) {
      setError('اتصال ناموفق: ' + (err?.message || 'نام کاربری یا رمز اشتباه است'))
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <h1>
          High<span className="dot">Music</span>
        </h1>
        <p>برای ورود، نام کاربری و رمز خود را وارد کنید</p>

        <div className="field">
          <label>نام کاربری</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} dir="ltr" autoFocus />
        </div>
        <div className="field">
          <label>رمز عبور</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            dir="ltr"
          />
        </div>

        <button className="btn" type="submit" disabled={busy}>
          {busy ? 'در حال اتصال...' : 'ورود'}
        </button>
        {error && <div className="login-error">{error}</div>}

        <div style={{ textAlign: 'center', marginTop: 18, fontSize: 13, color: 'var(--text-dim)' }}>
          حساب ندارید؟{' '}
          <a href={SIGNUP_TELEGRAM} style={{ color: 'var(--accent)', fontWeight: 700 }}>
            ثبت‌نام از طریق تلگرام
          </a>
        </div>
      </form>
    </div>
  )
}
