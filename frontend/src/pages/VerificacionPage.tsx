import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import EstadoBadge from '../components/EstadoBadge'
import {
  ETIQUETAS_MOTIVO_DICTAMEN,
  extractErrorMessage,
  listarPendientesVerificacion,
  marcarIlegible,
  obtenerDetalleVerificacion,
  obtenerImagenActaUrl,
  validarVerificacion,
  type ActaResponse,
  type MotivoDictamenVerificador,
  type VerificacionDetalleResponse,
} from '../lib/api'

const MOTIVOS = Object.entries(ETIQUETAS_MOTIVO_DICTAMEN) as [MotivoDictamenVerificador, string][]

/** Todas las claves (partidos + especiales) que aparecen en cualquiera de las capturas, en orden estable. */
function clavesUnificadas(capturas: VerificacionDetalleResponse['capturas']): string[] {
  const vistas = new Set<string>()
  const claves: string[] = []
  for (const c of capturas) {
    for (const clave of Object.keys(c.votos)) {
      if (!vistas.has(clave)) {
        vistas.add(clave)
        claves.push(clave)
      }
    }
  }
  return claves
}

/** Una clave está en discrepancia si no todas las capturas que la reportan coinciden en su valor. */
function hayDiscrepancia(capturas: VerificacionDetalleResponse['capturas'], clave: string): boolean {
  const valores = capturas.map((c) => c.votos[clave]).filter((v) => v !== undefined)
  return new Set(valores).size > 1
}

export default function VerificacionPage() {
  const [pendientes, setPendientes] = useState<ActaResponse[]>([])
  const [detalle, setDetalle] = useState<VerificacionDetalleResponse | null>(null)
  const [motivoCatalogo, setMotivoCatalogo] = useState<MotivoDictamenVerificador | ''>('')
  const [justificacion, setJustificacion] = useState('')
  const [confirmandoIlegible, setConfirmandoIlegible] = useState(false)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const [imagenUrl, setImagenUrl] = useState<string | null>(null)
  const [imagenError, setImagenError] = useState(false)
  const [zoom, setZoom] = useState(1)
  const [rotacion, setRotacion] = useState(0)

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
    setMotivoCatalogo('')
    setJustificacion('')
    setConfirmandoIlegible(false)
    setZoom(1)
    setRotacion(0)
    try {
      const d = await obtenerDetalleVerificacion(actaId)
      setDetalle(d)
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  // Trae la foto del acta en deliberación; revoca la anterior para no fugar memoria.
  useEffect(() => {
    setImagenError(false)
    setImagenUrl(null)
    if (!detalle) return
    let cancelado = false
    let url: string | null = null
    obtenerImagenActaUrl(detalle.acta.id)
      .then((u) => {
        if (cancelado) {
          URL.revokeObjectURL(u)
          return
        }
        url = u
        setImagenUrl(u)
      })
      .catch(() => {
        if (!cancelado) setImagenError(true)
      })
    return () => {
      cancelado = true
      if (url) URL.revokeObjectURL(url)
    }
  }, [detalle?.acta.id])

  const justificacionValida = motivoCatalogo !== '' && justificacion.trim().length > 0

  const elegirCaptura = async (numeroCaptura: number) => {
    if (!detalle || motivoCatalogo === '' || !justificacion.trim()) return
    setError(null)
    setCargando(true)
    try {
      await validarVerificacion(detalle.acta.id, numeroCaptura, motivoCatalogo, justificacion.trim())
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
    if (!detalle || motivoCatalogo === '' || !justificacion.trim()) return
    setError(null)
    setCargando(true)
    try {
      await marcarIlegible(detalle.acta.id, motivoCatalogo, justificacion.trim())
      setMensaje(`Acta ${detalle.acta.id} marcada como ilegible.`)
      setDetalle(null)
      setConfirmandoIlegible(false)
      await cargarPendientes()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  const claves = detalle ? clavesUnificadas(detalle.capturas) : []

  return (
    <AppShell titulo="Mesa de deliberación" ancho="max-w-6xl">
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
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2 lg:items-start">
          {/* Columna izquierda: imagen del acta con zoom y rotación */}
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:sticky lg:top-6">
            <div className="mb-3 flex items-center justify-between">
              <p className="text-sm font-medium text-slate-500">Imagen del acta física</p>
              <div className="flex gap-1">
                <button
                  type="button"
                  onClick={() => setZoom((z) => Math.max(1, z - 0.25))}
                  className="rounded-lg border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50"
                  title="Alejar"
                >
                  −
                </button>
                <button
                  type="button"
                  onClick={() => setZoom((z) => Math.min(3, z + 0.25))}
                  className="rounded-lg border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50"
                  title="Acercar"
                >
                  +
                </button>
                <button
                  type="button"
                  onClick={() => setRotacion((r) => (r + 90) % 360)}
                  className="rounded-lg border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50"
                  title="Rotar 90°"
                >
                  ⟳
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setZoom(1)
                    setRotacion(0)
                  }}
                  className="rounded-lg border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50"
                  title="Restablecer"
                >
                  Reset
                </button>
              </div>
            </div>
            <div className="flex min-h-[400px] items-center justify-center overflow-auto rounded-xl bg-slate-50 p-2">
              {imagenError && (
                <p className="px-4 text-center text-sm text-slate-400">
                  No se pudo cargar la fotografía de esta acta.
                </p>
              )}
              {!imagenError && !imagenUrl && <p className="text-sm text-slate-400">Cargando fotografía…</p>}
              {!imagenError && imagenUrl && (
                <img
                  src={imagenUrl}
                  alt={`Acta digitalizada de la casilla ${detalle.acta.casillaId}`}
                  className="max-w-none transition-transform"
                  style={{
                    transform: `rotate(${rotacion}deg) scale(${zoom})`,
                    maxHeight: zoom === 1 ? '75vh' : undefined,
                    width: zoom === 1 ? '100%' : undefined,
                    objectFit: 'contain',
                  }}
                  onError={() => setImagenError(true)}
                />
              )}
            </div>
          </div>

          {/* Columna derecha: comparación de capturas + dictamen */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-6 flex items-center justify-between">
              <div>
                <p className="text-sm text-slate-500">Acta #{detalle.acta.id}</p>
                <p className="text-lg font-semibold text-impepac-ink">Casilla {detalle.acta.casillaId}</p>
              </div>
              <EstadoBadge estado={detalle.acta.estado} />
            </div>

            <div className="mb-6 overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-3 py-2">Clave</th>
                    {detalle.capturas.map((c) => (
                      <th key={c.numeroCaptura} className="px-3 py-2 text-right">
                        Captura {c.numeroCaptura}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {claves.map((clave) => {
                    const discrepa = hayDiscrepancia(detalle.capturas, clave)
                    return (
                      <tr key={clave} className={discrepa ? 'bg-amber-50' : undefined}>
                        <td className="px-3 py-2 font-medium text-impepac-ink">
                          {clave}
                          {discrepa && <span className="ml-2 text-xs text-amber-600">⚠ discrepancia</span>}
                        </td>
                        {detalle.capturas.map((c) => (
                          <td
                            key={c.numeroCaptura}
                            className={`px-3 py-2 text-right ${discrepa ? 'font-semibold text-amber-700' : 'text-slate-600'}`}
                          >
                            {c.votos[clave] ?? '—'}
                          </td>
                        ))}
                      </tr>
                    )
                  })}
                  <tr className="bg-slate-50 font-medium">
                    <td className="px-3 py-2 text-impepac-ink">Total anotado en el acta</td>
                    {detalle.capturas.map((c) => (
                      <td key={c.numeroCaptura} className="px-3 py-2 text-right text-slate-600">
                        {c.totalVotosActa ?? '—'}
                      </td>
                    ))}
                  </tr>
                </tbody>
              </table>
            </div>

            <div className="mb-4 space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div>
                <label htmlFor="motivo" className="mb-1 block text-sm font-medium text-impepac-ink">
                  Motivo (catálogo)
                </label>
                <select
                  id="motivo"
                  value={motivoCatalogo}
                  onChange={(e) => setMotivoCatalogo(e.target.value as MotivoDictamenVerificador)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
                >
                  <option value="">Selecciona un motivo…</option>
                  {MOTIVOS.map(([valor, etiqueta]) => (
                    <option key={valor} value={valor}>
                      {etiqueta}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="justificacion" className="mb-1 block text-sm font-medium text-impepac-ink">
                  Justificación
                </label>
                <textarea
                  id="justificacion"
                  value={justificacion}
                  onChange={(e) => setJustificacion(e.target.value)}
                  rows={2}
                  placeholder="Describe brevemente el criterio de tu dictamen…"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
                />
              </div>
              {!justificacionValida && (
                <p className="text-xs text-slate-400">
                  El motivo y la justificación son obligatorios antes de guardar cualquier dictamen.
                </p>
              )}
            </div>

            <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
              {detalle.capturas.map((c) => (
                <button
                  key={c.numeroCaptura}
                  type="button"
                  onClick={() => elegirCaptura(c.numeroCaptura)}
                  disabled={cargando || !justificacionValida}
                  className="rounded-lg bg-impepac-magenta-600 px-3 py-2 text-xs font-medium text-white hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Captura {c.numeroCaptura} coincide con el acta
                </button>
              ))}
            </div>

            <div className="mt-4 border-t border-slate-100 pt-4">
              <button
                type="button"
                onClick={() => setConfirmandoIlegible(true)}
                disabled={cargando || !justificacionValida}
                className="w-full rounded-lg border-2 border-impepac-magenta-600 px-4 py-2.5 text-sm font-semibold text-impepac-magenta-700 hover:bg-impepac-magenta-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Declarar Ilegible
              </button>
            </div>

            <button
              type="button"
              onClick={() => setDetalle(null)}
              className="mt-4 text-sm text-slate-400 hover:text-impepac-ink"
            >
              ← Volver a la lista
            </button>
          </div>
        </div>
      )}

      {confirmandoIlegible && detalle && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="mb-2 text-lg font-semibold text-impepac-ink">¿Declarar ilegible el acta #{detalle.acta.id}?</h3>
            <p className="mb-4 text-sm text-slate-600">
              Esta etiqueta es definitiva: el acta se publicará como ilegible sin sumar votos a ningún
              candidato. Esta acción no se puede deshacer.
            </p>
            <div className="mb-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
              <p>
                <span className="font-medium text-slate-700">Motivo:</span>{' '}
                {motivoCatalogo ? ETIQUETAS_MOTIVO_DICTAMEN[motivoCatalogo] : '—'}
              </p>
              <p className="mt-1">
                <span className="font-medium text-slate-700">Justificación:</span> {justificacion}
              </p>
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirmandoIlegible(false)}
                disabled={cargando}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={onIlegible}
                disabled={cargando}
                className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700 disabled:opacity-60"
              >
                {cargando ? 'Guardando…' : 'Sí, declarar ilegible'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}
