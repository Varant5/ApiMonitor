import { lazy, Suspense, type ReactNode } from 'react'
import { HashRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'

// The monitor page pulls in the charting library — load it only when someone opens a monitor.
const MonitorPage = lazy(() => import('./pages/MonitorPage').then((m) => ({ default: m.MonitorPage })))

/*
 * HashRouter (URLs like /#/monitors/1): the server only ever serves index.html at "/",
 * so Spring Boot needs no extra "forward unknown paths to index.html" configuration.
 */
export function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <Protected>
                <DashboardPage />
              </Protected>
            }
          />
          <Route
            path="/monitors/:id"
            element={
              <Protected>
                <MonitorPage />
              </Protected>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </HashRouter>
    </AuthProvider>
  )
}

function Protected({ children }: { children: ReactNode }) {
  const { token, email, logout } = useAuth()

  if (!token) return <Navigate to="/login" replace />

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          <img src="/favicon.svg" alt="" width={24} height={24} />
          API Monitor
        </Link>
        <div className="topbar__user">
          <span className="muted small">{email}</span>
          <button className="button button--small" onClick={logout}>
            Log out
          </button>
        </div>
      </header>
      <main className="container">
        <Suspense fallback={<p className="muted">Loading…</p>}>{children}</Suspense>
      </main>
    </>
  )
}
