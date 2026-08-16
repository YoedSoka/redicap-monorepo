import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import EstadoBadge from '../components/EstadoBadge'
import {
  extractErrorMessage,
  listarPendientesVerificacion,
  marcarIlegible,
  obtenerDetalleVerificacion,
  validarVerificacion,
  type ActaResponse,
  type VerificacionDetalleResponse,
} from '../lib/api'

export default function VerificacionPage() {
  const [pendientes, setPendientes] = useState<ActaResponse[]>([])
  const [detalle, setDetalle] = useState<VerificacionDetalleResponse | null>(null)
  const [motivo, setMotivo] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargarPendientes = async () => {
    try {
      const lista = await listarPendientesVerificacion()
      setPendientes(lista)
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  useEffect(() => {
    cargarPendientes()
  }, [])

  const abrirDetalle = async (actaId: number) => {
    setError(null)
    setMensaje(null)
    setMotivo('')
    try {
      const d = await obtenerDetalleVerificacion(actaId)
      setDetalle(d)
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  const elegirCaptura = async (numeroCaptura: number) => {
    if (!detalle) return
    setError(null)
    setCargando(true)
    try {
      await validarVerificacion(detalle.acta.id, numeroCaptura)
      setMensaje(`Acta ${detalle.acta.id} validada con la captura ${numeroCaptura}.`)
      setDetalle(null)
      await cargarPendientes()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  const onIlegible = async () => {
    if (!detalle || !motivo.trim()) return
    setError(null)
    setCargando(true)
    try {
      await marcarIlegible(detalle.acta.id, motivo.trim())
      setMensaje(`Acta ${detalle.acta.id} marcada como ilegible.`)
      setDetalle(null)
      await cargarPendientes()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <AppShell titulo="Mesa de deliberación">
      {mensaje && (
        <p className="mb-4 rounded-lg bg-impepac-purple-50 px-4 py-3 text-sm text-impepac-purple-700">
          {mensaje}
        </p>
      )}
      {error && (
        <p className="mb-4 rounded-lg bg-impepac-magenta-50 px-4 py-3 text-sm text-impepac-magenta-700">
          {error}
        </p>
      )}

      {!detalle && (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-medium text-slate-500">
            Actas pendientes de deliberación ({pendientes.length})
          </h2>
          {pendientes.length === 0 ? (
            <p className="text-sm text-slate-500">No hay actas pendientes por ahora.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {pendientes.map((acta) => (
                <li key={acta.id} className="flex items-center justify-between py-3">
                  <span className="text-sm text-impepac-ink">
                    Acta #{acta.id} · Casilla {acta.casillaId}
                  </span>
                  <button
                    type="button"
                    onClick={() => abrirDetalle(acta.id)}
                    className="rounded-lg border border-impepac-purple-300 px-3 py-1.5 text-sm text-impepac-purple-700 hover:bg-impepac-purple-50"
                  >
                    Deliberar
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {detalle && (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-500">Acta #{detalle.acta.id}</p>
              <p className="text-lg font-semibold text-impepac-ink">Casilla {detalle.acta.casillaId}</p>
            </div>
            <EstadoBadge estado={detalle.acta.estado} />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            {detalle.capturas.map((c) => (
              <div key={c.numeroCaptura} className="rounded-xl border border-slate-200 p-4">
                <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-400">
                  Captura {c.numeroCaptura}
                </p>
                <ul className="mb-3 space-y-1 text-sm text-impepac-ink">
                  {Object.entries(c.votos).map(([clave, votos]) => (
                    <li key={clave} className="flex justify-between">
                      <span className="text-slate-500">{clave}</span>
                      <span className="font-medium">{votos}</span>
                    </li>
                  ))}
                </ul>
                <p className="mb-3 text-xs text-slate-400">Total: {c.totalVotosCalculado}</p>
                <button
                  type="button"
                  onClick={() => elegirCaptura(c.numeroCaptura)}
                  disabled={cargando}
                  className="w-full rounded-lg bg-impepac-magenta-600 px-3 py-2 text-xs font-medium text-white hover:bg-impepac-magenta-700 disabled:opacity-60"
                >
                  Esta coincide con el acta física
                </button>
              </div>
            ))}
          </div>

          <div className="mt-6 border-t border-slate-100 pt-6">
            <label htmlFor="motivo" className="mb-1 block text-sm font-medium text-impepac-ink">
              Ninguna coincide: marcar ilegible
            </label>
            <div className="flex gap-2">
              <input
                id="motivo"
                type="text"
                placeholder="Motivo"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
              <button
                type="button"
                onClick={onIlegible}
                disabled={cargando || !motivo.trim()}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                Marcar ilegible
              </button>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setDetalle(null)}
            className="mt-4 text-sm text-slate-400 hover:text-impepac-ink"
          >
            ← Volver a la lista
          </button>
        </div>
      )}
    </AppShell>
  )
}
