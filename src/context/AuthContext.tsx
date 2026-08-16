import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../lib/api'

interface Sesion {
  username: string
  rol: string
}

interface AuthContextValue {
  sesion: Sesion | null
  cargando: boolean
  iniciarSesion: (username: string, password: string) => Promise<string>
  cerrarSesion: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function leerSesionGuardada(): Sesion | null {
  const token = localStorage.getItem('redicap.token')
  const username = localStorage.getItem('redicap.username')
  const rol = localStorage.getItem('redicap.rol')
  if (token && username && rol) {
    return { username, rol }
  }
  return null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(leerSesionGuardada)
  const [cargando, setCargando] = useState(false)

  const iniciarSesion = async (username: string, password: string) => {
    setCargando(true)
    try {
      const resp = await authApi.login(username, password)
      localStorage.setItem('redicap.token', resp.token)
      localStorage.setItem('redicap.username', resp.username)
      localStorage.setItem('redicap.rol', resp.rol)
      setSesion({ username: resp.username, rol: resp.rol })
      return resp.rol
    } finally {
      setCargando(false)
    }
  }

  const cerrarSesion = async () => {
    try {
      await authApi.logout()
    } catch {
      // el token puede haber expirado; igual limpiamos la sesión local
    }
    localStorage.removeItem('redicap.token')
    localStorage.removeItem('redicap.username')
    localStorage.removeItem('redicap.rol')
    setSesion(null)
  }

  const value = useMemo(() => ({ sesion, cargando, iniciarSesion, cerrarSesion }), [sesion, cargando])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  }
  return ctx
}
