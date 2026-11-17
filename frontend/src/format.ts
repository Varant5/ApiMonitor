/**
 * The backend sends LocalDateTime without a time zone ("2026-11-17T13:50:09.629").
 * We show it as-is instead of converting, so it always matches the server's clock.
 */
export function formatDateTime(value: string | null): string {
  if (!value) return '—'
  return value.slice(0, 19).replace('T', ' ')
}

export function formatTime(value: string): string {
  return value.slice(11, 16)
}

export function formatPercent(value: number): string {
  return `${value.toFixed(value === 100 || value === 0 ? 0 : 1)}%`
}

export function formatMs(value: number | null): string {
  return value === null ? '—' : `${Math.round(value)} ms`
}
