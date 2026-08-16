import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { rutaPorRol } from '../lib/roles'

export default function RutaProtegida({
  children,
  rolesPermitidos,
}: {
  children: ReactNode
  rolesPermitidos?: string[]
}) {
  const { sesion } = useAuth()
  if (!sesion) {
    return <Navigate to="/login" replace />
  }
  if (rolesPermitidos && !rolesPermitidos.includes(sesion.rol)) {
    return <Navigate to={rutaPorRol(sesion.rol)} replace />
  }
  return <>{children}</>
}
