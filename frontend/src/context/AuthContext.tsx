import { createContext, useContext, useState, type ReactNode } from 'react'
import type { UserInfo } from '../types'

interface AuthContextValue {
  user: UserInfo | null
  login: (token: string, userInfo: UserInfo) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(() => {
    const token = localStorage.getItem('token')
    const info = localStorage.getItem('userInfo')
    if (token && info) return JSON.parse(info) as UserInfo
    return null
  })

  function login(token: string, userInfo: UserInfo) {
    localStorage.setItem('token', token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    setUser(userInfo)
  }

  function logout() {
    localStorage.clear()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
