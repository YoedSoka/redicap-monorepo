import { useEffect, useState, type FormEvent } from 'react'
import AppShell from '../components/AppShell'
import AdminNav from '../components/AdminNav'
import { NOMBRE_ROL, ROLES } from '../lib/roles'
import {
  actualizarUsuario,
  cambiarActivoUsuario,
  crearUsuario,
  desbloquearUsuario,
  extractErrorMessage,
  listarCasillas,
  listarUsuarios,
  type CasillaResponse,
  type Rol,
  type UsuarioResponse,
} from '../lib/api'

interface FormState {
  username: string
  password: string
  nombreCompleto: string
  curp: string
  rol: Rol
  casillaAsignadaId: string
}

const FORM_VACIO: FormState = {
  username: '',
  password: '',
  nombreCompleto: '',
  curp: '',
  rol: 'CAPTURISTA',
  casillaAsignadaId: '',
}

export default function AdminUsuariosPage() {
  const [usuarios, setUsuarios] = useState<UsuarioResponse[]>([])
  const [casillas, setCasillas] = useState<CasillaResponse[]>([])
  const [formAbierto, setFormAbierto] = useState(false)
  const [editando, setEditando] = useState<UsuarioResponse | null>(null)
  const [form, setForm] = useState<FormState>(FORM_VACIO)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargar = async () => {
    try {
      const [u, c] = await Promise.all([listarUsuarios(), listarCasillas()])
      setUsuarios(u)
      setCasillas(c)
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

  const abrirEditar = (u: UsuarioResponse) => {
    setEditando(u)
    setForm({
      username: u.username,
      password: '',
      nombreCompleto: u.nombreCompleto,
      curp: u.curp ?? '',
      rol: u.rol,
      casillaAsignadaId: u.casillaAsignadaId ? String(u.casillaAsignadaId) : '',
    })
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
      const casillaAsignadaId = form.casillaAsignadaId ? Number(form.casillaAsignadaId) : undefined
      if (editando) {
        await actualizarUsuario(editando.id, {
          nombreCompleto: form.nombreCompleto,
          curp: form.curp || undefined,
          rol: form.rol,
          casillaAsignadaId,
        })
        setMensaje(`Usuario ${editando.username} actualizado.`)
      } else {
        await crearUsuario({
          username: form.username,
          password: form.password,
          nombreCompleto: form.nombreCompleto,
          curp: form.curp || undefined,
          rol: form.rol,
          casillaAsignadaId,
        })
        setMensaje(`Usuario ${form.username} creado.`)
      }
      cerrarForm()
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  const onToggleActivo = async (u: UsuarioResponse) => {
    setError(null)
    try {
      await cambiarActivoUsuario(u.id, !u.activo)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  const onDesbloquear = async (u: UsuarioResponse) => {
    setError(null)
    try {
      await desbloquearUsuario(u.id)
      setMensaje(`Usuario ${u.username} desbloqueado.`)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  return (
    <AppShell titulo="Gestión de usuarios" ancho="max-w-5xl">
      <AdminNav />

      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-impepac-ink">Usuarios ({usuarios.length})</h2>
        <button
          type="button"
          onClick={abrirCrear}
          className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
        >
          + Nuevo usuario
        </button>
      </div>

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

      {formAbierto && (
        <form
          onSubmit={onSubmit}
          className="mb-6 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2"
        >
          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">Usuario</label>
            <input
              type="text"
              required
              disabled={!!editando}
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500 disabled:bg-slate-50 disabled:text-slate-400"
            />
          </div>

          {!editando && (
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Contraseña</label>
              <input
                type="password"
                required
                minLength={8}
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
          )}

          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">Nombre completo</label>
            <input
              type="text"
              required
              value={form.nombreCompleto}
              onChange={(e) => setForm({ ...form, nombreCompleto: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">CURP (opcional)</label>
            <input
              type="text"
              value={form.curp}
              onChange={(e) => setForm({ ...form, curp: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">Rol</label>
            <select
              value={form.rol}
              onChange={(e) => setForm({ ...form, rol: e.target.value as Rol })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {NOMBRE_ROL[r]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-impepac-ink">
              Casilla asignada (digitalizador)
            </label>
            <select
              value={form.casillaAsignadaId}
              onChange={(e) => setForm({ ...form, casillaAsignadaId: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
            >
              <option value="">Sin asignar</option>
              {casillas.map((c) => (
                <option key={c.id} value={c.id}>
                  Sección {c.numeroSeccion} · {c.tipo} {c.numeroCasilla}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-end gap-2 sm:col-span-2">
            <button
              type="submit"
              disabled={cargando}
              className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700 disabled:opacity-60"
            >
              {cargando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear usuario'}
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
              <th className="px-4 py-3">Usuario</th>
              <th className="px-4 py-3">Nombre</th>
              <th className="px-4 py-3">Rol</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {usuarios.map((u) => {
              const bloqueado = !!u.bloqueadoHasta && new Date(u.bloqueadoHasta) > new Date()
              return (
                <tr key={u.id} className="transition-colors hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-impepac-ink">{u.username}</td>
                  <td className="px-4 py-3 text-slate-600">{u.nombreCompleto}</td>
                  <td className="px-4 py-3 text-slate-600">{NOMBRE_ROL[u.rol] ?? u.rol}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-col items-start gap-1">
                      <span
                        className={
                          u.activo
                            ? 'rounded-full bg-impepac-purple-50 px-2 py-1 text-xs font-medium text-impepac-purple-700'
                            : 'rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-500'
                        }
                      >
                        {u.activo ? 'Activo' : 'Inactivo'}
                      </span>
                      {bloqueado && (
                        <span className="rounded-full bg-impepac-magenta-50 px-2 py-1 text-xs font-medium text-impepac-magenta-700">
                          Bloqueado
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2 text-xs">
                      <button
                        type="button"
                        onClick={() => abrirEditar(u)}
                        className="rounded-lg border border-impepac-purple-100 px-2 py-1 font-medium text-impepac-purple-700 transition-colors hover:bg-impepac-purple-50"
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        onClick={() => onToggleActivo(u)}
                        className="rounded-lg border border-slate-200 px-2 py-1 font-medium text-slate-500 transition-colors hover:bg-slate-100"
                      >
                        {u.activo ? 'Desactivar' : 'Activar'}
                      </button>
                      {bloqueado && (
                        <button
                          type="button"
                          onClick={() => onDesbloquear(u)}
                          className="rounded-lg border border-impepac-magenta-100 px-2 py-1 font-medium text-impepac-magenta-700 transition-colors hover:bg-impepac-magenta-50"
                        >
                          Desbloquear
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </AppShell>
  )
}
