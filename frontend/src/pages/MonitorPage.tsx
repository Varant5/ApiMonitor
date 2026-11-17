import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError, api } from '../api'
import { ResponseTimeChart } from '../components/ResponseTimeChart'
import { StatTile } from '../components/StatTile'
import { StatusBadge } from '../components/StatusBadge'
import { formatDateTime, formatMs, formatPercent } from '../format'
import { usePolling } from '../usePolling'

const CHART_CHECKS = 100
const TABLE_CHECKS = 20

export function MonitorPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()

  const load = useCallback(
    () => Promise.all([api.monitor(id), api.statistics(id), api.checks(id, CHART_CHECKS)]),
    [id],
  )
  const { data, error, refresh } = usePolling(load, 30_000)

  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)

  async function run(action: () => Promise<unknown>, after: () => void = refresh) {
    setActionError(null)
    setBusy(true)
    try {
      await action()
      after()
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : 'Could not reach the server.')
    } finally {
      setBusy(false)
    }
  }

  if (error && !data) {
    return (
      <div className="card empty">
        <h2>Monitor not available</h2>
        <p className="muted">{error}</p>
        <Link to="/" className="button">
          ← Back to dashboard
        </Link>
      </div>
    )
  }

  if (!data) return <p className="muted">Loading…</p>

  const [monitor, stats, checks] = data
  const latest = checks[0] ?? null

  return (
    <>
      <Link to="/" className="back-link">
        ← Dashboard
      </Link>

      <div className="page-header">
        <div>
          <h1 className="with-badge">
            {monitor.name} <StatusBadge status={latest?.status ?? null} enabled={monitor.enabled} />
          </h1>
          <a href={monitor.url} target="_blank" rel="noreferrer" className="muted truncate">
            {monitor.url}
          </a>
          <p className="muted small">Checked every {monitor.checkIntervalSeconds} s</p>
        </div>

        <div className="actions">
          <button className="button button--primary" disabled={busy} onClick={() => run(() => api.checkNow(id))}>
            Check now
          </button>
          <button className="button" disabled={busy} onClick={() => run(() => api.setEnabled(id, !monitor.enabled))}>
            {monitor.enabled ? 'Pause' : 'Resume'}
          </button>
          {confirmDelete ? (
            <span className="confirm">
              Delete with all history?
              <button
                className="button button--danger"
                disabled={busy}
                onClick={() => run(() => api.deleteMonitor(id), () => navigate('/'))}
              >
                Yes, delete
              </button>
              <button className="button" onClick={() => setConfirmDelete(false)}>
                Cancel
              </button>
            </span>
          ) : (
            <button className="button" onClick={() => setConfirmDelete(true)}>
              Delete
            </button>
          )}
        </div>
      </div>

      {actionError && (
        <p className="alert" role="alert">
          {actionError}
        </p>
      )}

      <section className="tiles" aria-label="Statistics">
        <StatTile label="Uptime" value={stats.totalChecks > 0 ? formatPercent(stats.uptime) : '—'} />
        <StatTile label="Avg response" value={formatMs(stats.totalChecks > 0 ? stats.averageResponseTimeMs : null)} />
        <StatTile
          label="Fastest / slowest"
          value={
            stats.totalChecks > 0 ? `${stats.minResponseTimeMs} / ${stats.maxResponseTimeMs} ms` : '—'
          }
        />
        <StatTile label="Failed checks" value={stats.failedChecks} hint={`of ${stats.totalChecks} total`} />
      </section>

      <section className="card">
        <h2 className="card__title">Response time — last {Math.min(checks.length, CHART_CHECKS)} checks</h2>
        <ResponseTimeChart checks={checks} />
      </section>

      <section className="card table-wrap">
        <h2 className="card__title">Recent checks</h2>
        {checks.length === 0 ? (
          <p className="muted">No checks yet. Click “Check now” or wait for the scheduler.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Status</th>
                <th className="num">HTTP</th>
                <th className="num">Response</th>
              </tr>
            </thead>
            <tbody>
              {checks.slice(0, TABLE_CHECKS).map((check) => (
                <tr key={check.id}>
                  <td className="small">{formatDateTime(check.checkedAt)}</td>
                  <td>
                    <StatusBadge status={check.status} />
                  </td>
                  <td className="num">{check.httpStatusCode ?? '—'}</td>
                  <td className="num">{formatMs(check.responseTimeMs)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}
