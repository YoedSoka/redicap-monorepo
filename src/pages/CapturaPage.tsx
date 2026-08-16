import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import EstadoBadge from '../components/EstadoBadge'
import {
  extractErrorMessage,
  obtenerSiguienteActa,
  registrarCaptura,
  type ActaResponse,
} from '../lib/api'

interface Fila {
  clave: string
  votos: string
}

const FILAS_INICIALES: Fila[] = [
  { clave: '', votos: '' },
  { clave: 'NULOS', votos: '' },
  { clave: 'NO_REGISTRADOS', votos: '' },
]

export default function CapturaPage() {
  const [acta, setActa] = useState<ActaResponse | null | undefined>(undefined)
  const [filas, setFilas] = useState<Fila[]>(FILAS_INICIALES)
  const [totalVotosActa, setTotalVotosActa] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargarSiguiente = async () => {
    setError(null)
    setMensaje(null)
    setActa(undefined)
    setFilas(FILAS_INICIALES)
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

  const actualizarFila = (i: number, campo: keyof Fila, valor: string) => {
    setFilas((prev) => prev.map((f, idx) => (idx === i ? { ...f, [campo]: valor } : f)))
  }

  const agregarFila = () => setFilas((prev) => [...prev, { clave: '', votos: '' }])
  const quitarFila = (i: number) => setFilas((prev) => prev.filter((_, idx) => idx !== i))

  const onSubmit = async () => {
    if (!acta) return
    setError(null)
    setCargando(true)
    try {
      const votos: Record<string, number> = {}
      for (const fila of filas) {
        const clave = fila.clave.trim().toUpperCase()
        if (!clave) continue
        votos[clave] = Number(fila.votos) || 0
      }
      const actualizada = await registrarCaptura(
        acta.id,
        votos,
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
    <AppShell titulo="Captura de actas">
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
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-500">Acta #{acta.id}</p>
              <p className="text-lg font-semibold text-impepac-ink">Casilla {acta.casillaId}</p>
            </div>
            <EstadoBadge estado={acta.estado} />
          </div>

          <div className="space-y-3">
            {filas.map((fila, i) => (
              <div key={i} className="flex items-center gap-2">
                <input
                  type="text"
                  placeholder="Partido / NULOS / NO_REGISTRADOS"
                  value={fila.clave}
                  onChange={(e) => actualizarFila(i, 'clave', e.target.value)}
                  className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
                />
                <input
                  type="number"
                  min={0}
                  placeholder="Votos"
                  value={fila.votos}
                  onChange={(e) => actualizarFila(i, 'votos', e.target.value)}
                  className="w-28 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
                />
                <button
                  type="button"
                  onClick={() => quitarFila(i)}
                  className="px-2 text-slate-400 hover:text-impepac-magenta-600"
                  aria-label="Quitar fila"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>

          <button
            type="button"
            onClick={agregarFila}
            className="mt-3 text-sm font-medium text-impepac-purple-700 hover:underline"
          >
            + Agregar partido
          </button>

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

          <button
            type="button"
            onClick={onSubmit}
            disabled={cargando}
            className="mt-6 w-full rounded-lg bg-impepac-magenta-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {cargando ? 'Guardando…' : 'Guardar captura'}
          </button>
        </div>
      )}
    </AppShell>
  )
}
