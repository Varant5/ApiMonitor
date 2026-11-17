import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Loads data and reloads it every `intervalMs` (while the tab is visible),
 * so the dashboard follows the scheduler without a manual refresh.
 */
export function usePolling<T>(load: () => Promise<T>, intervalMs: number) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const loadRef = useRef(load)
  loadRef.current = load

  const refresh = useCallback(async () => {
    try {
      setData(await loadRef.current())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong.')
    }
  }, [])

  useEffect(() => {
    refresh()
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') refresh()
    }, intervalMs)
    return () => window.clearInterval(timer)
  }, [refresh, intervalMs])

  return { data, error, refresh }
}
