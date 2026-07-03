import React, { useEffect, useState } from 'react'
import { useApp } from '../store'
import { Artist, getArtists } from '../api'
import { Cover } from '../components/common'

export function Artists() {
  const { creds, navigate } = useApp()
  const [artists, setArtists] = useState<Artist[]>([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')

  useEffect(() => {
    if (!creds) return
    setLoading(true)
    getArtists(creds)
      .then(setArtists)
      .catch(() => setArtists([]))
      .finally(() => setLoading(false))
  }, [creds])

  const shown = q
    ? artists.filter((a) => a.name.toLowerCase().includes(q.toLowerCase()))
    : artists

  return (
    <div>
      <div className="page-title">هنرمندان</div>
      <input
        className="search-input"
        placeholder="فیلتر هنرمندان..."
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />
      {loading ? (
        <div className="empty">در حال بارگذاری...</div>
      ) : (
        <div className="grid" style={{ marginTop: 20 }}>
          {shown.map((a) => (
            <div key={a.id} className="card round" onClick={() => navigate('artist', a.id)}>
              <Cover id={a.coverArt} />
              <div className="card-title">{a.name}</div>
              <div className="card-sub">{a.albumCount || 0} آلبوم</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
