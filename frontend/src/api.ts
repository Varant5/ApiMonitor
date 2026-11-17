// Types mirror the backend DTOs (MonitorResponse, MonitorCheckResponse, ...).

export type Status = 'UP' | 'DOWN'

export interface Monitor {
  id: number
  name: string
  url: string
  checkIntervalSeconds: number
  enabled: boolean
  createdAt: string
}

export interface MonitorCheck {
  id: number
  monitorId: number
  status: Status
  httpStatusCode: number | null
  responseTimeMs: number | null
  checkedAt: string
}

export interface MonitorStatistics {
  totalChecks: number
  failedChecks: number
  uptime: number
  averageResponseTimeMs: number
  minResponseTimeMs: number
  maxResponseTimeMs: number
}

export interface MonitorSummary {
  id: number
  name: string
  url: string
  enabled: boolean
  checkIntervalSeconds: number
  lastStatus: Status | null
  lastCheckedAt: string | null
  lastResponseTimeMs: number | null
  lastHttpStatusCode: number | null
  uptime: number
  totalChecks: number
}

export interface Dashboard {
  totalMonitors: number
  up: number
  down: number
  pending: number
  paused: number
  overallUptime: number
  monitors: MonitorSummary[]
}

export interface NewMonitor {
  name: string
  url: string
  checkIntervalSeconds: number
}

/** Same shape as the backend ErrorResponse. */
interface ErrorBody {
  status: number
  message: string
  errors: string[]
}

export class ApiError extends Error {
  readonly status: number
  readonly errors: string[]

  constructor(status: number, body?: ErrorBody) {
    super(body?.errors?.join(' ') || body?.message || `Request failed (HTTP ${status})`)
    this.status = status
    this.errors = body?.errors ?? []
  }
}

// ---------- token storage ----------

const TOKEN_KEY = 'api-monitor.token'

export const tokenStore = {
  get: (): string | null => {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set: (token: string) => {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* private mode: the user stays logged in until the tab is closed */
    }
  },
  clear: () => {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* ignore */
    }
  },
}

/** Called when a request with a token gets 401 (expired or invalid token). */
let onUnauthorized: () => void = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// ---------- request helper ----------

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = tokenStore.get()

  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`/api${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 401 && token) {
    onUnauthorized()
  }

  if (!response.ok) {
    let errorBody: ErrorBody | undefined
    try {
      errorBody = await response.json()
    } catch {
      /* not JSON */
    }
    throw new ApiError(response.status, errorBody)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

// ---------- endpoints ----------

export const api = {
  register: (email: string, password: string) =>
    request<{ id: number; email: string }>('POST', '/auth/register', { email, password }),

  login: (email: string, password: string) =>
    request<{ accessToken: string; tokenType: string; expiresIn: number }>('POST', '/auth/login', {
      email,
      password,
    }),

  dashboard: () => request<Dashboard>('GET', '/dashboard'),

  monitor: (id: number) => request<Monitor>('GET', `/monitors/${id}`),

  createMonitor: (monitor: NewMonitor) => request<Monitor>('POST', '/monitors', monitor),

  setEnabled: (id: number, enabled: boolean) =>
    request<Monitor>('PATCH', `/monitors/${id}`, { enabled }),

  deleteMonitor: (id: number) => request<void>('DELETE', `/monitors/${id}`),

  checkNow: (id: number) => request<MonitorCheck>('POST', `/monitors/${id}/check`),

  checks: (id: number, limit: number) =>
    request<MonitorCheck[]>('GET', `/monitors/${id}/checks?limit=${limit}`),

  statistics: (id: number) => request<MonitorStatistics>('GET', `/monitors/${id}/statistics`),
}
