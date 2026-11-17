import { Link } from 'react-router-dom'
import { api } from '../api'
import { AddMonitorForm } from '../components/AddMonitorForm'
import { StatTile } from '../components/StatTile'
import { StatusBadge } from '../components/StatusBadge'
import { formatDateTime, formatMs, formatPercent } from '../format'
import { usePolling } from '../usePolling'

export function DashboardPage() {
  const { data, error, refresh } = usePolling(api.dashboard, 30_000)

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Refreshes automatically every 30 seconds.</p>
        </div>
        <AddMonitorForm onCreated={refresh} />
      </div>

      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}

      {data && (
        <>
          <section className="tiles" aria-label="Summary">
            <StatTile label="Monitors" value={data.totalMonitors} />
            <StatTile label="Up" value={data.up} />
            <StatTile label="Down" value={data.down} hint={data.down > 0 ? 'Needs attention' : undefined} />
            <StatTile
              label="Overall uptime"
              value={formatPercent(data.overallUptime)}
              hint={data.pending + data.paused > 0 ? `${data.pending} pending · ${data.paused} paused` : undefined}
            />
          </section>

          {data.monitors.length === 0 ? (
            <div className="card empty">
              <h2>No monitors yet</h2>
              <p className="muted">Add the first URL you want to watch — it will be checked automatically.</p>
            </div>
          ) : (
            <div className="card table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Monitor</th>
                    <th className="num">Uptime</th>
                    <th className="num">Response</th>
                    <th>Last check</th>
                  </tr>
                </thead>
                <tbody>
                  {data.monitors.map((m) => (
                    <tr key={m.id}>
                      <td>
                        <StatusBadge status={m.lastStatus} enabled={m.enabled} />
                      </td>
                      <td>
                        <Link to={`/monitors/${m.id}`} className="monitor-link">
                          {m.name}
                        </Link>
                        <div className="muted small truncate">{m.url}</div>
                      </td>
                      <td className="num">{m.totalChecks > 0 ? formatPercent(m.uptime) : '—'}</td>
                      <td className="num">{formatMs(m.lastResponseTimeMs)}</td>
                      <td className="small">{formatDateTime(m.lastCheckedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  )
}
