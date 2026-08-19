import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import {
  ELECCIONES,
  ETIQUETAS_ELECCION,
  extractErrorMessage,
  listarPartidos,
  obtenerHistorialCortes,
  obtenerUltimoCorte,
  type CorteResponse,
  type PartidoPoliticoResponse,
  type TipoEleccion,
} from '../lib/api'

const COLOR_DEFECTO = '#d81b60'
const ACENTOS_TILE = [
  'border-t-impepac-magenta-500',
  'border-t-impepac-purple-500',
  'border-t-impepac-magenta-500',
  'border-t-impepac-purple-500',
]

function formatearFecha(iso: string): string {
  return new Date(iso).toLocaleString('es-MX', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export default function ConsultaPage() {
  const [eleccion, setEleccion] = useState<TipoEleccion>('GUBERNATURA')
  const [ultimo, setUltimo] = useState<CorteResponse | null | undefined>(undefined)
  const [historial, setHistorial] = useState<CorteResponse[]>([])
  const [partidos, setPartidos] = useState<Map<string, PartidoPoliticoResponse>>(new Map())
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelado = false
    const cargar = async () => {
      setUltimo(undefined)
      setError(null)
      try {
        const [u, h, p] = await Promise.all([
          obtenerUltimoCorte(eleccion),
          obtenerHistorialCortes(eleccion),
          listarPartidos(),
        ])
        if (cancelado) return
        setUltimo(u)
        setHistorial(h)
        setPartidos(new Map(p.map((partido) => [partido.siglas, partido])))
      } catch (err) {
        if (cancelado) return
        setError(extractErrorMessage(err))
        setUltimo(null)
      }
    }
    cargar()
    return () => {
      cancelado = true
    }
  }, [eleccion])

  const totalVotos = ultimo ? Object.values(ultimo.resultados).reduce((a, b) => a + b, 0) : 0
  const resultadosOrdenados = ultimo
    ? Object.entries(ultimo.resultados).sort(([, a], [, b]) => b - a)
    : []

  return (
    <AppShell titulo="Consulta de resultados" ancho="max-w-4xl">
      <div className="mb-6 flex gap-2 rounded-xl bg-slate-100 p-1">
        {ELECCIONES.map((e) => (
          <button
            key={e}
            type="button"
            onClick={() => setEleccion(e)}
            className={`flex-1 rounded-lg px-3 py-2 text-sm font-medium transition ${
              eleccion === e
                ? 'bg-white text-impepac-magenta-700 shadow-sm'
                : 'text-slate-500 hover:text-impepac-ink'
            }`}
          >
            {ETIQUETAS_ELECCION[e]}
          </button>
        ))}
      </div>

      {error && (
        <p className="mb-4 rounded-lg bg-impepac-magenta-50 px-4 py-3 text-sm text-impepac-magenta-700">
          {error}
        </p>
      )}

      {ultimo === undefined && <p className="text-sm text-slate-500">Cargando resultados…</p>}

      {ultimo === null && !error && (
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <p className="text-slate-600">
            Aún no se ha publicado ningún corte de resultados para {ETIQUETAS_ELECCION[eleccion]}.
          </p>
        </div>
      )}

      {ultimo && (
        <>
          <p className="mb-4 text-sm text-slate-500">
            Último corte: {formatearFecha(ultimo.generadoAt)}
          </p>

          <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
            {[
              ['Casillas', ultimo.totalCasillas],
              ['Capturadas', ultimo.totalActasCapturadas],
              ['Validadas', ultimo.totalActasValidadas],
              [
                'Participación',
                ultimo.porcentajeParticipacion != null ? `${ultimo.porcentajeParticipacion}%` : '—',
              ],
            ].map(([etiqueta, valor], i) => (
              <div
                key={etiqueta}
                className={`rounded-2xl border border-t-4 border-slate-200 bg-white p-4 shadow-sm ${ACENTOS_TILE[i]}`}
              >
                <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{etiqueta}</p>
                <p className="mt-1 text-2xl font-semibold text-impepac-ink">{valor}</p>
              </div>
            ))}
          </div>

          <div className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-sm font-medium text-slate-500">
              Resultados por partido ({totalVotos} votos)
            </h2>
            <div className="space-y-3">
              {resultadosOrdenados.map(([clave, votos]) => {
                const pct = totalVotos > 0 ? (votos / totalVotos) * 100 : 0
                const partido = partidos.get(clave)
                const color = partido?.colorHex ?? COLOR_DEFECTO
                return (
                  <div key={clave}>
                    <div className="mb-1 flex justify-between text-sm">
                      <span className="inline-flex items-center gap-2">
                        <span
                          className="h-2.5 w-2.5 shrink-0 rounded-full border border-slate-200"
                          style={{ backgroundColor: color }}
                        />
                        <span className="font-medium text-impepac-ink">{clave}</span>
                        {partido && <span className="text-xs text-slate-400">{partido.nombre}</span>}
                      </span>
                      <span className="text-slate-500">
                        {votos} ({pct.toFixed(1)}%)
                      </span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full rounded-full"
                        style={{ width: `${pct}%`, backgroundColor: color }}
                      />
                    </div>
                  </div>
                )
              })}
              {resultadosOrdenados.length === 0 && (
                <p className="text-sm text-slate-500">Sin votos registrados todavía.</p>
              )}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
            <h2 className="px-6 pt-6 text-sm font-medium text-slate-500">Historial de cortes</h2>
            <table className="mt-4 w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-6 py-3">Fecha</th>
                  <th className="px-6 py-3">Actas validadas</th>
                  <th className="px-6 py-3">Participación</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {historial.map((c) => (
                  <tr key={c.id} className="transition-colors hover:bg-slate-50">
                    <td className="px-6 py-3 text-impepac-ink">{formatearFecha(c.generadoAt)}</td>
                    <td className="px-6 py-3 text-slate-600">
                      {c.totalActasValidadas} / {c.totalCasillas}
                    </td>
                    <td className="px-6 py-3 text-slate-600">
                      {c.porcentajeParticipacion != null ? `${c.porcentajeParticipacion}%` : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </AppShell>
  )
}
