import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from './api'

interface AuthState {
  token: string | null
  email: string | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

/** Reads the payload of a JWT (no verification — the server does that). */
function decodeToken(token: string): { email?: string; exp?: number } | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

function isUsable(token: string | null): token is string {
  if (!token) return false
  const exp = decodeToken(token)?.exp
  return exp === undefined || exp * 1000 > Date.now()
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    const stored = tokenStore.get()
    return isUsable(stored) ? stored : null
  })

  const logout = useCallback(() => {
    tokenStore.clear()
    setToken(null)
  }, [])

  // an expired token anywhere in the app logs the user out
  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await api.login(email, password)
    tokenStore.set(accessToken)
    setToken(accessToken)
  }, [])

  const register = useCallback(
    async (email: string, password: string) => {
      await api.register(email, password)
      await login(email, password)
    },
    [login],
  )

  const value = useMemo<AuthState>(
    () => ({
      token,
      email: token ? (decodeToken(token)?.email ?? null) : null,
      login,
      register,
      logout,
    }),
    [token, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
