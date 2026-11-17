/*
 * The API returns instants in UTC ("2026-11-17T13:15:21.123Z").
 * They are shown in the browser's own time zone, so every user sees their local time
 * no matter where the server runs.
 */

// "sv-SE" formats as "2026-11-17 14:15:21" — unambiguous and sortable; the zone is the browser's.
const dateTimeFormat = new Intl.DateTimeFormat('sv-SE', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
})

const timeFormat = new Intl.DateTimeFormat('sv-SE', { hour: '2-digit', minute: '2-digit' })

export function formatDateTime(value: string | null): string {
  if (!value) return '—'
  return dateTimeFormat.format(new Date(value))
}

export function formatTime(value: string): string {
  return timeFormat.format(new Date(value))
}

export function formatPercent(value: number): string {
  return `${value.toFixed(value === 100 || value === 0 ? 0 : 1)}%`
}

export function formatMs(value: number | null): string {
  return value === null ? '—' : `${Math.round(value)} ms`
}
