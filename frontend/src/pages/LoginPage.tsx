import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../lib/api'
import { rutaPorRol } from '../lib/roles'

export default function LoginPage() {
  const { iniciarSesion, cargando } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      const rol = await iniciarSesion(username, password)
      navigate(rutaPorRol(rol))
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg">
        <div
          className="px-8 py-10 text-center"
          style={{
            backgroundImage:
              'linear-gradient(180deg, var(--color-impepac-purple-900) 0%, var(--color-impepac-purple-500) 40%, var(--color-impepac-purple-300) 76%, #ffffff 100%)',
          }}
        >
          <p className="text-sm font-medium tracking-wide text-white/90">IMPEPAC Morelos</p>
          <h1 className="mt-1 text-2xl font-semibold text-white">REDICAP</h1>
          <p className="mt-1 text-xs text-white/80">
            Registro, Digitalización, Captura y Publicación de Actas
          </p>
        </div>

        <form onSubmit={onSubmit} className="space-y-5 px-8 py-8">
          <div>
            <label htmlFor="username" className="mb-1 block text-sm font-medium text-impepac-ink">
              Usuario
            </label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500 focus:ring-2 focus:ring-impepac-magenta-100"
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-impepac-ink">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-impepac-magenta-500 focus:ring-2 focus:ring-impepac-magenta-100"
            />
          </div>

          {error && (
            <p className="rounded-lg bg-impepac-magenta-50 px-3 py-2 text-sm text-impepac-magenta-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={cargando}
            className="w-full rounded-lg bg-impepac-magenta-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-impepac-magenta-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {cargando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
      </div>
    </div>
  )
}
