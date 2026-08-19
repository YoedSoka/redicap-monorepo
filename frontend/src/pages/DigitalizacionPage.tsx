import { useState, type FormEvent } from 'react'
import AppShell from '../components/AppShell'
import EstadoBadge from '../components/EstadoBadge'
import {
  ELECCIONES,
  ETIQUETAS_ELECCION,
  extractErrorMessage,
  subirActaDigitalizada,
  type ActaResponse,
  type TipoEleccion,
} from '../lib/api'

export default function DigitalizacionPage() {
  const [casillaId, setCasillaId] = useState('')
  const [tipoEleccion, setTipoEleccion] = useState<TipoEleccion>('GUBERNATURA')
  const [archivo, setArchivo] = useState<File | null>(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ActaResponse | null>(null)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!archivo) {
      setError('Selecciona la imagen del acta')
      return
    }
    setError(null)
    setResultado(null)
    setCargando(true)
    try {
      const acta = await subirActaDigitalizada(Number(casillaId), tipoEleccion, archivo)
      setResultado(acta)
      setCasillaId('')
      setArchivo(null)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <AppShell titulo="Digitalización de actas">
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <form onSubmit={onSubmit} className="space-y-5">
          <div>
            <label htmlFor="casillaId" className="mb-1 block text-sm font-medium text-impepac-ink">
              Casilla
            </label>
            <input
              id="casillaId"
              type="number"
              required
              value={casillaId}
              onChange={(e) => setCasillaId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            />
          </div>

          <div>
            <label htmlFor="tipoEleccion" className="mb-1 block text-sm font-medium text-impepac-ink">
              Elección
            </label>
            <select
              id="tipoEleccion"
              value={tipoEleccion}
              onChange={(e) => setTipoEleccion(e.target.value as TipoEleccion)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            >
              {ELECCIONES.map((e) => (
                <option key={e} value={e}>
                  {ETIQUETAS_ELECCION[e]}
                </option>
              ))}
            </select>
            <p className="mt-1 text-xs text-slate-400">
              Cada casilla produce un acta independiente por cada elección.
            </p>
          </div>

          <div>
            <label htmlFor="imagen" className="mb-1 block text-sm font-medium text-impepac-ink">
              Fotografía del acta
            </label>
            <input
              id="imagen"
              type="file"
              accept="image/jpeg,image/png"
              required
              onChange={(e) => setArchivo(e.target.files?.[0] ?? null)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none file:mr-3 file:rounded-md file:border-0 file:bg-impepac-purple-100 file:px-3 file:py-1.5 file:text-impepac-purple-700"
            />
            <p className="mt-1 text-xs text-slate-400">
              El hash SHA-256 se calcula en tu dispositivo antes de subir la imagen.
            </p>
          </div>

          {error && (
            <p className="rounded-lg bg-impepac-magenta-50 px-3 py-2 text-sm text-impepac-magenta-700">
              {error}
            </p>
          )}

          {resultado && (
            <div className="flex items-center justify-between rounded-lg bg-impepac-purple-50 px-3 py-2 text-sm text-impepac-purple-700">
              <span>Acta #{resultado.id} recibida (casilla {resultado.casillaId})</span>
              <EstadoBadge estado={resultado.estado} />
            </div>
          )}

          <button
            type="submit"
            disabled={cargando}
            className="w-full rounded-lg bg-impepac-magenta-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {cargando ? 'Subiendo…' : 'Subir acta'}
          </button>
        </form>
      </div>
    </AppShell>
  )
}
