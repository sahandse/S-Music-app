import React from 'react'
import { Cover } from './common'
import { fmtDuration, MusicVideoItem } from '../musicVideos'
import { t } from '../i18n'

// Shared thumbnail tile for a music video — used on the Home shelf and in
// the full Music Videos grid, so both look identical (YouTube/Aparat style:
// 16:9 cover, genre + duration badges, title/artist below).
export function MusicVideoCard({
  video,
  onClick,
  className = ''
}: {
  video: MusicVideoItem
  onClick: () => void
  className?: string
}) {
  return (
    <div className={`mv-card ${className}`} onClick={onClick}>
      <div className="mv-thumb">
        <Cover directUrl={video.coverUrl} size={320} />
        {video.genre && <span className="mv-badge">{video.genre}</span>}
        {video.durationSec > 0 && <span className="mv-duration">{fmtDuration(video.durationSec)}</span>}
      </div>
      <div className="card-title" style={{ marginTop: 8 }}>
        {video.title || t('mv.untitled')}
      </div>
      {video.artist && <div className="card-sub">{video.artist}</div>}
    </div>
  )
}
