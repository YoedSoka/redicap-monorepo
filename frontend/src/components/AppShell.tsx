import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { NOMBRE_ROL } from '../lib/roles'

export default function AppShell({
  titulo,
  ancho = 'max-w-3xl',
  children,
}: {
  titulo: string
  ancho?: string
  children: ReactNode
}) {
  const { sesion, cerrarSesion } = useAuth()
  const navigate = useNavigate()

  const onLogout = async () => {
    await cerrarSesion()
    navigate('/login')
  }

  return (
    <div className="min-h-screen">
      <header
        className="flex items-center justify-between px-6 py-4 text-white"
        style={{
          backgroundImage:
            'linear-gradient(180deg, var(--color-impepac-purple-900) 0%, var(--color-impepac-purple-500) 40%, var(--color-impepac-purple-300) 76%, #ffffff 100%)',
        }}
      >
        <div>
          <p className="text-xs text-white/80">IMPEPAC Morelos</p>
          <h1 className="text-lg font-semibold">{titulo}</h1>
        </div>
        <div className="flex items-center gap-3">
          {sesion && (
            <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-medium">
              {sesion.username} · {NOMBRE_ROL[sesion.rol] ?? sesion.rol}
            </span>
          )}
          <button
            type="button"
            onClick={onLogout}
            className="rounded-lg border border-white/40 px-3 py-1.5 text-sm transition hover:bg-white/10"
          >
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className={`mx-auto px-6 py-10 ${ancho}`}>{children}</main>
    </div>
  )
}
