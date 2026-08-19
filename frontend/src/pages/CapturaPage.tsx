import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import EstadoBadge from '../components/EstadoBadge'
import {
  extractErrorMessage,
  listarPartidos,
  obtenerImagenActaUrl,
  obtenerSiguienteActa,
  registrarCaptura,
  type ActaResponse,
  type PartidoPoliticoResponse,
} from '../lib/api'

const CLAVES_ESPECIALES = ['NULOS', 'NO_REGISTRADOS'] as const

export default function CapturaPage() {
  const [acta, setActa] = useState<ActaResponse | null | undefined>(undefined)
  const [partidos, setPartidos] = useState<PartidoPoliticoResponse[]>([])
  const [votos, setVotos] = useState<Record<string, string>>({})
  const [totalVotosActa, setTotalVotosActa] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [imagenUrl, setImagenUrl] = useState<string | null>(null)
  const [imagenError, setImagenError] = useState(false)

  useEffect(() => {
    listarPartidos()
      .then((todos) => setPartidos(todos.filter((p) => p.activo)))
      .catch((err) => setError(extractErrorMessage(err)))
  }, [])

  const cargarSiguiente = async () => {
    setError(null)
    setMensaje(null)
    setActa(undefined)
    setVotos({})
    setTotalVotosActa('')
    try {
      const siguiente = await obtenerSiguienteActa()
      setActa(siguiente)
    } catch (err) {
      setError(extractErrorMessage(err))
      setActa(null)
    }
  }

  useEffect(() => {
    cargarSiguiente()
  }, [])

  // Trae la foto del acta actual como referencia visual; revoca la anterior para no fugar memoria.
  useEffect(() => {
    setImagenError(false)
    if (!acta) {
      setImagenUrl(null)
      return
    }
    let cancelado = false
    let url: string | null = null
    obtenerImagenActaUrl(acta.id)
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
  }, [acta])

  const claves = [...partidos.map((p) => p.siglas), ...CLAVES_ESPECIALES]
  const camposVacios = claves.filter((clave) => (votos[clave] ?? '').trim() === '')

  const onSubmit = async () => {
    if (!acta) return
    if (camposVacios.length > 0) {
      setError('Captura el número de votos de cada partido (usa 0 si el acta lo registra en cero); no puedes dejar campos en blanco.')
      return
    }
    setError(null)
    setCargando(true)
    try {
      const votosNumericos: Record<string, number> = {}
      for (const clave of claves) {
        votosNumericos[clave] = Number(votos[clave])
      }
      const actualizada = await registrarCaptura(
        acta.id,
        votosNumericos,
        totalVotosActa ? Number(totalVotosActa) : undefined,
      )
      setMensaje(`Captura guardada. El acta ${actualizada.id} pasó a ${actualizada.estado}.`)
      await cargarSiguiente()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <AppShell titulo="Captura de actas" ancho="max-w-6xl">
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

      {acta === undefined && <p className="text-sm text-slate-500">Buscando acta disponible…</p>}

      {acta === null && !error && (
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <p className="text-slate-600">No hay actas disponibles para capturar en este momento.</p>
          <button
            type="button"
            onClick={cargarSiguiente}
            className="mt-4 rounded-lg border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          >
            Buscar de nuevo
          </button>
        </div>
      )}

      {acta && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2 lg:items-start">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:sticky lg:top-6">
            <p className="mb-3 text-sm font-medium text-slate-500">Fotografía del acta (referencia)</p>
            <div className="flex min-h-[300px] items-center justify-center overflow-hidden rounded-xl bg-slate-50">
              {imagenError && (
                <p className="px-4 text-center text-sm text-slate-400">
                  No se pudo cargar la fotografía de esta acta.
                </p>
              )}
              {!imagenError && !imagenUrl && (
                <p className="text-sm text-slate-400">Cargando fotografía…</p>
              )}
              {!imagenError && imagenUrl && (
                <img
                  src={imagenUrl}
                  alt={`Acta digitalizada de la casilla ${acta.casillaId}`}
                  className="max-h-[75vh] w-full object-contain"
                  onError={() => setImagenError(true)}
                />
              )}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-500">Acta #{acta.id}</p>
              <p className="text-lg font-semibold text-impepac-ink">Casilla {acta.casillaId}</p>
            </div>
            <EstadoBadge estado={acta.estado} />
          </div>

          {partidos.length === 0 && (
            <p className="mb-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
              No hay partidos configurados todavía — pídele al administrador que los cargue en Gestión de
              partidos. Por ahora solo puedes capturar nulos y no registrados.
            </p>
          )}

          <div className="space-y-3">
            {partidos.map((p) => (
              <div key={p.siglas} className="flex items-center gap-2">
                <span
                  className="h-3 w-3 shrink-0 rounded-full border border-slate-200"
                  style={{ backgroundColor: p.colorHex ?? '#e2e8f0' }}
                />
                <span className="flex-1 text-sm font-medium text-impepac-ink">
                  {p.siglas}
                  <span className="ml-2 text-xs font-normal text-slate-400">{p.nombre}</span>
                </span>
                <input
                  type="number"
                  min={0}
                  placeholder="Votos"
                  value={votos[p.siglas] ?? ''}
                  onChange={(e) => setVotos({ ...votos, [p.siglas]: e.target.value })}
                  className={`w-28 rounded-lg border px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500 ${
                    (votos[p.siglas] ?? '').trim() === '' ? 'border-impepac-magenta-300' : 'border-slate-300'
                  }`}
                />
              </div>
            ))}

            <div className="border-t border-slate-100 pt-3">
              {CLAVES_ESPECIALES.map((clave) => (
                <div key={clave} className="mb-3 flex items-center gap-2 last:mb-0">
                  <span className="flex-1 text-sm font-medium text-slate-500">{clave}</span>
                  <input
                    type="number"
                    min={0}
                    placeholder="Votos"
                    value={votos[clave] ?? ''}
                    onChange={(e) => setVotos({ ...votos, [clave]: e.target.value })}
                    className={`w-28 rounded-lg border px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500 ${
                      (votos[clave] ?? '').trim() === '' ? 'border-impepac-magenta-300' : 'border-slate-300'
                    }`}
                  />
                </div>
              ))}
            </div>
          </div>

          <div className="mt-6">
            <label className="mb-1 block text-sm font-medium text-impepac-ink">
              Total de votos anotado en el acta física
            </label>
            <input
              type="number"
              min={0}
              value={totalVotosActa}
              onChange={(e) => setTotalVotosActa(e.target.value)}
              className="w-40 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            />
          </div>

          {camposVacios.length > 0 && (
            <p className="mt-4 text-xs text-impepac-magenta-600">
              Faltan {camposVacios.length} campo(s) de votos por llenar. Escribe 0 si el acta registra cero
              votos para ese partido; no se puede enviar la captura con campos en blanco.
            </p>
          )}

          <button
            type="button"
            onClick={onSubmit}
            disabled={cargando || camposVacios.length > 0}
            className="mt-6 w-full rounded-lg bg-impepac-magenta-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {cargando ? 'Guardando…' : 'Guardar captura'}
          </button>
          </div>
        </div>
      )}
    </AppShell>
  )
}
