import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AppShell from '../components/AppShell'
import { rutaPorRol } from '../lib/roles'

export default function DashboardPage() {
  const { sesion } = useAuth()

  if (sesion && rutaPorRol(sesion.rol) !== '/') {
    return <Navigate to={rutaPorRol(sesion.rol)} replace />
  }

  const esDigitalizador = sesion?.rol === 'DIGITALIZADOR'

  return (
    <AppShell titulo="REDICAP">
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-500">Sesión iniciada como</p>
        <p className="mt-1 text-xl font-semibold text-impepac-ink">{sesion?.username}</p>
        {esDigitalizador ? (
          <p className="mt-4 text-sm text-slate-500">
            La digitalización de actas solo está disponible en la aplicación móvil REDICAP.
            La captura debe hacerse forzosamente con la cámara en tiempo real (DFR R1): no
            existe una vía web para subir imágenes desde la galería del dispositivo.
          </p>
        ) : (
          <p className="mt-4 text-sm text-slate-500">
            Todavía no hay una pantalla específica para este rol.
          </p>
        )}
      </div>
    </AppShell>
  )
}
