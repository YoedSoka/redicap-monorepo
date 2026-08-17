import { useEffect, useState, type FormEvent } from 'react'
import AppShell from '../components/AppShell'
import AdminNav from '../components/AdminNav'
import {
  crearCasilla,
  crearDistrito,
  crearMunicipio,
  crearSeccion,
  extractErrorMessage,
  listarCasillas,
  listarDistritos,
  listarMunicipios,
  listarSecciones,
  type CasillaResponse,
  type DistritoResponse,
  type MunicipioResponse,
  type SeccionResponse,
  type TipoCasilla,
} from '../lib/api'

const TIPOS_CASILLA: TipoCasilla[] = ['BASICA', 'CONTIGUA', 'ESPECIAL', 'EXTRAORDINARIA']

export default function AdminCatalogoPage() {
  const [distritos, setDistritos] = useState<DistritoResponse[]>([])
  const [municipios, setMunicipios] = useState<MunicipioResponse[]>([])
  const [secciones, setSecciones] = useState<SeccionResponse[]>([])
  const [casillas, setCasillas] = useState<CasillaResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargar = async () => {
    try {
      const [d, m, s, c] = await Promise.all([
        listarDistritos(),
        listarMunicipios(),
        listarSecciones(),
        listarCasillas(),
      ])
      setDistritos(d)
      setMunicipios(m)
      setSecciones(s)
      setCasillas(c)
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  useEffect(() => {
    cargar()
  }, [])

  // ── Distritos ─────────────────────────────────────────────────────────
  const [distritoFormAbierto, setDistritoFormAbierto] = useState(false)
  const [distritoForm, setDistritoForm] = useState({ clave: '', nombre: '', cabeceraDistrital: '' })

  const onSubmitDistrito = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await crearDistrito({
        clave: distritoForm.clave,
        nombre: distritoForm.nombre,
        cabeceraDistrital: distritoForm.cabeceraDistrital || undefined,
      })
      setMensaje(`Distrito ${distritoForm.clave.toUpperCase()} creado.`)
      setDistritoForm({ clave: '', nombre: '', cabeceraDistrital: '' })
      setDistritoFormAbierto(false)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  // ── Municipios ────────────────────────────────────────────────────────
  const [municipioFormAbierto, setMunicipioFormAbierto] = useState(false)
  const [municipioForm, setMunicipioForm] = useState({ clave: '', nombre: '' })

  const onSubmitMunicipio = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await crearMunicipio(municipioForm)
      setMensaje(`Municipio ${municipioForm.clave.toUpperCase()} creado.`)
      setMunicipioForm({ clave: '', nombre: '' })
      setMunicipioFormAbierto(false)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  // ── Secciones ─────────────────────────────────────────────────────────
  const [seccionFormAbierto, setSeccionFormAbierto] = useState(false)
  const [seccionForm, setSeccionForm] = useState({ numeroSeccion: '', municipioId: '', distritoId: '' })

  const onSubmitSeccion = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await crearSeccion({
        numeroSeccion: Number(seccionForm.numeroSeccion),
        municipioId: Number(seccionForm.municipioId),
        distritoId: Number(seccionForm.distritoId),
      })
      setMensaje(`Sección ${seccionForm.numeroSeccion} creada.`)
      setSeccionForm({ numeroSeccion: '', municipioId: '', distritoId: '' })
      setSeccionFormAbierto(false)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  // ── Casillas ──────────────────────────────────────────────────────────
  const [casillaFormAbierto, setCasillaFormAbierto] = useState(false)
  const [casillaForm, setCasillaForm] = useState({
    seccionId: '',
    tipo: 'BASICA' as TipoCasilla,
    numeroCasilla: '',
    listaNominal: '',
  })

  const onSubmitCasilla = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await crearCasilla({
        seccionId: Number(casillaForm.seccionId),
        tipo: casillaForm.tipo,
        numeroCasilla: Number(casillaForm.numeroCasilla),
        listaNominal: Number(casillaForm.listaNominal),
      })
      setMensaje(`Casilla ${casillaForm.tipo} ${casillaForm.numeroCasilla} creada.`)
      setCasillaForm({ seccionId: '', tipo: 'BASICA', numeroCasilla: '', listaNominal: '' })
      setCasillaFormAbierto(false)
      await cargar()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  return (
    <AppShell titulo="Catálogo geográfico" ancho="max-w-5xl">
      <AdminNav />

      <p className="mb-4 text-sm text-slate-500">
        Distrito y Municipio son catálogos independientes; cada Sección une un número de sección con un
        Municipio y un Distrito, y cada Casilla cuelga de una Sección. Aquí solo se puede dar de alta y
        consultar — no hay edición ni baja para estos catálogos.
      </p>

      {mensaje && (
        <p className="mb-4 rounded-lg bg-impepac-purple-50 px-4 py-3 text-sm text-impepac-purple-700">{mensaje}</p>
      )}
      {error && (
        <p className="mb-4 rounded-lg bg-impepac-magenta-50 px-4 py-3 text-sm text-impepac-magenta-700">{error}</p>
      )}

      {/* Distritos */}
      <section className="mb-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-impepac-ink">Distritos ({distritos.length})</h2>
          <button
            type="button"
            onClick={() => setDistritoFormAbierto((v) => !v)}
            className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
          >
            + Nuevo distrito
          </button>
        </div>

        {distritoFormAbierto && (
          <form
            onSubmit={onSubmitDistrito}
            className="mb-4 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-3"
          >
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Clave</label>
              <input
                type="text"
                required
                value={distritoForm.clave}
                onChange={(e) => setDistritoForm({ ...distritoForm, clave: e.target.value })}
                placeholder="D01"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Nombre</label>
              <input
                type="text"
                required
                value={distritoForm.nombre}
                onChange={(e) => setDistritoForm({ ...distritoForm, nombre: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Cabecera distrital (opcional)</label>
              <input
                type="text"
                value={distritoForm.cabeceraDistrital}
                onChange={(e) => setDistritoForm({ ...distritoForm, cabeceraDistrital: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div className="flex items-end gap-2 sm:col-span-3">
              <button
                type="submit"
                className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
              >
                Crear distrito
              </button>
              <button
                type="button"
                onClick={() => setDistritoFormAbierto(false)}
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
                <th className="px-4 py-3">Clave</th>
                <th className="px-4 py-3">Nombre</th>
                <th className="px-4 py-3">Cabecera</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {distritos.map((d) => (
                <tr key={d.id} className="transition-colors hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-impepac-ink">{d.clave}</td>
                  <td className="px-4 py-3 text-slate-600">{d.nombre}</td>
                  <td className="px-4 py-3 text-slate-600">{d.cabeceraDistrital ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Municipios */}
      <section className="mb-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-impepac-ink">Municipios ({municipios.length})</h2>
          <button
            type="button"
            onClick={() => setMunicipioFormAbierto((v) => !v)}
            className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
          >
            + Nuevo municipio
          </button>
        </div>

        {municipioFormAbierto && (
          <form
            onSubmit={onSubmitMunicipio}
            className="mb-4 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2"
          >
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Clave</label>
              <input
                type="text"
                required
                value={municipioForm.clave}
                onChange={(e) => setMunicipioForm({ ...municipioForm, clave: e.target.value })}
                placeholder="MUN001"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Nombre</label>
              <input
                type="text"
                required
                value={municipioForm.nombre}
                onChange={(e) => setMunicipioForm({ ...municipioForm, nombre: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div className="flex items-end gap-2 sm:col-span-2">
              <button
                type="submit"
                className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
              >
                Crear municipio
              </button>
              <button
                type="button"
                onClick={() => setMunicipioFormAbierto(false)}
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
                <th className="px-4 py-3">Clave</th>
                <th className="px-4 py-3">Nombre</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {municipios.map((m) => (
                <tr key={m.id} className="transition-colors hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-impepac-ink">{m.clave}</td>
                  <td className="px-4 py-3 text-slate-600">{m.nombre}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Secciones */}
      <section className="mb-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-impepac-ink">Secciones ({secciones.length})</h2>
          <button
            type="button"
            onClick={() => setSeccionFormAbierto((v) => !v)}
            disabled={distritos.length === 0 || municipios.length === 0}
            className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            + Nueva sección
          </button>
        </div>

        {(distritos.length === 0 || municipios.length === 0) && (
          <p className="mb-4 text-xs text-slate-400">Da de alta al menos un distrito y un municipio primero.</p>
        )}

        {seccionFormAbierto && (
          <form
            onSubmit={onSubmitSeccion}
            className="mb-4 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-3"
          >
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Número de sección</label>
              <input
                type="number"
                min={1}
                required
                value={seccionForm.numeroSeccion}
                onChange={(e) => setSeccionForm({ ...seccionForm, numeroSeccion: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Municipio</label>
              <select
                required
                value={seccionForm.municipioId}
                onChange={(e) => setSeccionForm({ ...seccionForm, municipioId: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              >
                <option value="">Selecciona…</option>
                {municipios.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.clave} · {m.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Distrito</label>
              <select
                required
                value={seccionForm.distritoId}
                onChange={(e) => setSeccionForm({ ...seccionForm, distritoId: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              >
                <option value="">Selecciona…</option>
                {distritos.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.clave} · {d.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex items-end gap-2 sm:col-span-3">
              <button
                type="submit"
                className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
              >
                Crear sección
              </button>
              <button
                type="button"
                onClick={() => setSeccionFormAbierto(false)}
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
                <th className="px-4 py-3">Sección</th>
                <th className="px-4 py-3">Municipio</th>
                <th className="px-4 py-3">Distrito</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {secciones.map((s) => (
                <tr key={s.id} className="transition-colors hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-impepac-ink">Sección {s.numeroSeccion}</td>
                  <td className="px-4 py-3 text-slate-600">{s.municipioNombre}</td>
                  <td className="px-4 py-3 text-slate-600">{s.distritoNombre}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Casillas */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-impepac-ink">Casillas ({casillas.length})</h2>
          <button
            type="button"
            onClick={() => setCasillaFormAbierto((v) => !v)}
            disabled={secciones.length === 0}
            className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            + Nueva casilla
          </button>
        </div>

        {secciones.length === 0 && (
          <p className="mb-4 text-xs text-slate-400">Da de alta al menos una sección primero.</p>
        )}

        {casillaFormAbierto && (
          <form
            onSubmit={onSubmitCasilla}
            className="mb-4 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-4"
          >
            <div className="sm:col-span-2">
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Sección</label>
              <select
                required
                value={casillaForm.seccionId}
                onChange={(e) => setCasillaForm({ ...casillaForm, seccionId: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              >
                <option value="">Selecciona…</option>
                {secciones.map((s) => (
                  <option key={s.id} value={s.id}>
                    Sección {s.numeroSeccion} · {s.municipioNombre} · {s.distritoNombre}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Tipo</label>
              <select
                value={casillaForm.tipo}
                onChange={(e) => setCasillaForm({ ...casillaForm, tipo: e.target.value as TipoCasilla })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              >
                {TIPOS_CASILLA.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Número</label>
              <input
                type="number"
                min={1}
                required
                value={casillaForm.numeroCasilla}
                onChange={(e) => setCasillaForm({ ...casillaForm, numeroCasilla: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-impepac-ink">Lista nominal</label>
              <input
                type="number"
                min={1}
                required
                value={casillaForm.listaNominal}
                onChange={(e) => setCasillaForm({ ...casillaForm, listaNominal: e.target.value })}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500"
              />
            </div>
            <div className="flex items-end gap-2 sm:col-span-4">
              <button
                type="submit"
                className="rounded-lg bg-impepac-magenta-600 px-4 py-2 text-sm font-medium text-white hover:bg-impepac-magenta-700"
              >
                Crear casilla
              </button>
              <button
                type="button"
                onClick={() => setCasillaFormAbierto(false)}
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
                <th className="px-4 py-3">Sección</th>
                <th className="px-4 py-3">Tipo</th>
                <th className="px-4 py-3">Número</th>
                <th className="px-4 py-3">Lista nominal</th>
                <th className="px-4 py-3">Estado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {casillas.map((c) => (
                <tr key={c.id} className="transition-colors hover:bg-slate-50">
                  <td className="px-4 py-3 text-slate-600">
                    Sección {c.numeroSeccion} · {c.municipioNombre} · {c.distritoNombre}
                  </td>
                  <td className="px-4 py-3 font-medium text-impepac-ink">{c.tipo}</td>
                  <td className="px-4 py-3 text-slate-600">{c.numeroCasilla}</td>
                  <td className="px-4 py-3 text-slate-600">{c.listaNominal}</td>
                  <td className="px-4 py-3">
                    <span
                      className={
                        c.activa
                          ? 'rounded-full bg-impepac-purple-50 px-2 py-1 text-xs font-medium text-impepac-purple-700'
                          : 'rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-500'
                      }
                    >
                      {c.activa ? 'Activa' : 'Inactiva'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </AppShell>
  )
}
