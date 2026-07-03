import React, { useState } from 'react'
import { useApp } from '../store'
import { makeCreds, ping } from '../api'

export function Login() {
  const { setCreds } = useApp()
  const [url, setUrl] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!url || !username) {
      setError('آدرس سرور و نام کاربری لازم است')
      return
    }
    setBusy(true)
    const creds = makeCreds(url, username, password)
    try {
      await ping(creds)
      setCreds(creds)
    } catch (err: any) {
      setError('اتصال ناموفق: ' + (err?.message || 'سرور را بررسی کنید'))
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <h1>
          High<span className="dot">Music</span>
        </h1>
        <p>به سرور Subsonic / Navidrome خود وصل شوید</p>

        <div className="field">
          <label>آدرس سرور</label>
          <input
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://music.example.com"
            dir="ltr"
          />
        </div>
        <div className="field">
          <label>نام کاربری</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} dir="ltr" />
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
      </form>
    </div>
  )
}
