import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { MonitorCheck } from '../api'
import { formatDateTime, formatMs, formatTime } from '../format'

/**
 * Response time over time — one series, so no legend (the card title names it).
 * Failed checks get a red marker; the tooltip always spells out the status, so color is never the only cue.
 * Colors come from CSS variables (see .response-chart in styles.css), so light and dark mode both work.
 */
export function ResponseTimeChart({ checks }: { checks: MonitorCheck[] }) {
  // the API returns newest first; a time axis reads left -> right, oldest -> newest
  const data = [...checks].reverse()

  if (data.length < 2) {
    return <p className="muted">Not enough data yet — the chart appears after a few checks.</p>
  }

  return (
    <div className="response-chart" role="img" aria-label="Response time of the latest checks">
      <ResponsiveContainer width="100%" height={260}>
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} />
          <XAxis
            dataKey="checkedAt"
            tickFormatter={formatTime}
            tickLine={false}
            minTickGap={40}
          />
          <YAxis
            tickFormatter={(value: number) => `${value} ms`}
            tickLine={false}
            axisLine={false}
            width={64}
            allowDecimals={false}
          />
          <Tooltip content={ChartTooltip} isAnimationActive={false} />
          <Line
            type="monotone"
            dataKey="responseTimeMs"
            strokeWidth={2}
            dot={renderDot}
            activeDot={{ r: 5, className: 'chart-active-dot' }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

interface DotProps {
  cx?: number
  cy?: number
  index?: number
  payload?: MonitorCheck
}

/** Only failed checks get a marker; a ring in the surface color separates it from the line. */
function renderDot({ cx, cy, index, payload }: DotProps) {
  if (payload?.status !== 'DOWN' || cx === undefined || cy === undefined) {
    return <g key={`dot-${index}`} />
  }
  return <circle key={`dot-${index}`} cx={cx} cy={cy} r={4.5} className="chart-down-dot" />
}

interface TooltipProps {
  active?: boolean
  payload?: ReadonlyArray<{ payload?: MonitorCheck }>
}

function ChartTooltip({ active, payload }: TooltipProps) {
  const check = payload?.[0]?.payload
  if (!active || !check) return null

  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip__time">{formatDateTime(check.checkedAt)}</div>
      <div>
        <strong>{formatMs(check.responseTimeMs)}</strong>
      </div>
      <div className={check.status === 'DOWN' ? 'chart-tooltip__down' : 'chart-tooltip__up'}>
        {check.status === 'DOWN' ? '✕ Down' : '✓ Up'}
        {check.httpStatusCode !== null ? ` · HTTP ${check.httpStatusCode}` : ' · no response'}
      </div>
    </div>
  )
}
