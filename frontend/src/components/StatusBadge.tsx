import type { Status } from '../api'

type BadgeState = Status | 'PENDING' | 'PAUSED'

const LABELS: Record<BadgeState, { icon: string; text: string }> = {
  UP: { icon: '✓', text: 'Up' },
  DOWN: { icon: '✕', text: 'Down' },
  PENDING: { icon: '…', text: 'Pending' },
  PAUSED: { icon: '❚❚', text: 'Paused' },
}

/** Status is always icon + text, never color alone. */
export function StatusBadge({ status, enabled = true }: { status: Status | null; enabled?: boolean }) {
  const state: BadgeState = !enabled ? 'PAUSED' : (status ?? 'PENDING')
  const { icon, text } = LABELS[state]

  return (
    <span className={`badge badge--${state.toLowerCase()}`}>
      <span className="badge__icon" aria-hidden="true">
        {icon}
      </span>
      {text}
    </span>
  )
}
