import { useEffect, useState, type FormEvent } from 'react'
import AppShell from '../components/AppShell'
import AdminNav from '../components/AdminNav'
import {
  actualizarPartido,
  cambiarActivoPartido,
  crearPartido,
  extractErrorMessage,
  listarPartidos,
  type PartidoPoliticoResponse,
} from '../lib/api'

interface FormState {
  siglas: string
  nombre: string
  colorHex: string
}

const FORM_VACIO: FormState = { siglas: '', nombre: '', colorHex: '#DC2597' }

export default function AdminPartidosPage() {
  const [partidos, setPartidos] = useState<PartidoPoliticoResponse[]>([])
  const [formAbierto, setFormAbierto] = useState(false)
  const [editando, setEditando] = useState<PartidoPoliticoResponse | null>(null)
  const [form, setForm] = useState<FormState>(FORM_VACIO)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargar = async () => {
    try {
      setPartidos(await listarPartidos())
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  useEffect(() => {
    cargar()
  }, [])

  const abrirCrear = () => {
    setEditando(null)
    setForm(FORM_VACIO)
    setFormAbierto(true)
    setError(null)
  }

  const abrirEditar = (p: PartidoPoliticoResponse) => {
    setEditando(p)
    setForm({ siglas: p.siglas, nombre: p.nombre, colorHex: p.colorHex ?? '#DC2597' })
    setFormAbierto(true)
    setError(null)
  }

  const cerrarForm = () => {
    setFormAbierto(false)
    setEditando(null)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      if (editando) {
        await actualizarPartido(editando.id, { nombre: form.nombre, colorHex: form.colorHex || undefined })
        setMensaje(`Partido ${editando.siglas} actualizado.`)
      } else {
        await crearPartido({ siglas: form.siglas, nombre: form.nombre, colorHex: form.colorHex || undefined })
        setMensaje(`Partido ${form.siglas.toUpperCase()} creado.`)
      }
      cerrarForm()
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  const onToggleActivo = async (p: PartidoPoliticoResponse) => {
    setError(null)
    try {
      await cambiarActivoPartido(p.id, !p.activo)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  return (
    <AppShell titulo="Gestión de partidos" ancho="max-w-4xl">
      <AdminNav />

      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-impepac-ink">Partidos políticos ({partidos.length})</h2>
        <button
          type="button"
          onClick={abrirCrear}
          className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
        >
          + Nuevo partido
        </button>
      </div>

      <p className="mb-4 text-sm text-slate-500">
        Estos partidos son los que verán capturistas y verificadores al registrar votos. Confirma que la lista
        coincida con los partidos realmente registrados ante el IMPEPAC para el proceso electoral vigente antes
        de usarla en producción — no vino precargada con una fuente oficial.
      </p>

      {mensaje && (
        <p className="mb-4 rounded-lg bg-impepac-purple-50 px-4 py-3 text-sm text-impepac-purple-700">{mensaje}</p>
      )}
      {error && (
        <p className="mb-4 rounded-lg bg-impepac-magenta-50 px-4 py-3 text-sm text-impepac-magenta-700">{error}</p>
      )}

      {formAbierto && (
        <form
          onSubmit={onSubmit}
          className="mb-6 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-3"
        >
          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">Siglas</label>
            <input
              type="text"
              required
              disabled={!!editando}
              value={form.siglas}
              onChange={(e) => setForm({ ...form, siglas: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500 disabled:bg-slate-50 disabled:text-slate-400"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="mb-1 block text-sm font-medium text-impepac-ink">Nombre completo</label>
            <input
              type="text"
              required
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">Color</label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={form.colorHex}
                onChange={(e) => setForm({ ...form, colorHex: e.target.value })}
                className="h-10 w-14 rounded-lg border border-slate-300"
              />
              <input
                type="text"
                value={form.colorHex}
                onChange={(e) => setForm({ ...form, colorHex: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
          </div>

          <div className="flex items-end gap-2 sm:col-span-2">
            <button
              type="submit"
              disabled={cargando}
              className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700 disabled:opacity-60"
            >
              {cargando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear partido'}
            </button>
            <button
              type="button"
              onClick={cerrarForm}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
            >
              Cancelar
            </button>
          </div>
        </form>
      )}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3">Partido</th>
              <th className="px-4 py-3">Nombre</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {partidos.map((p) => (
              <tr key={p.id}>
                <td className="px-4 py-3">
                  <span className="inline-flex items-center gap-2 font-medium text-impepac-ink">
                    <span
                      className="h-3 w-3 rounded-full border border-slate-200"
                      style={{ backgroundColor: p.colorHex ?? '#e2e8f0' }}
                    />
                    {p.siglas}
                  </span>
                </td>
                <td className="px-4 py-3 text-slate-600">{p.nombre}</td>
                <td className="px-4 py-3">
                  <span
                    className={
                      p.activo
                        ? 'rounded-full bg-impepac-purple-50 px-2 py-1 text-xs font-medium text-impepac-purple-700'
                        : 'rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-500'
                    }
                  >
                    {p.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-3 text-xs">
                    <button
                      type="button"
                      onClick={() => abrirEditar(p)}
                      className="font-medium text-impepac-purple-700 hover:underline"
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      onClick={() => onToggleActivo(p)}
                      className="font-medium text-slate-500 hover:underline"
                    >
                      {p.activo ? 'Desactivar' : 'Activar'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AppShell>
  )
}
