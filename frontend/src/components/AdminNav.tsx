import { NavLink } from 'react-router-dom'

const enlaces = [
  { ruta: '/admin/usuarios', etiqueta: 'Usuarios' },
  { ruta: '/admin/partidos', etiqueta: 'Partidos' },
  { ruta: '/admin/catalogo', etiqueta: 'Catálogo' },
]

export default function AdminNav() {
  return (
    <div className="mb-6 flex gap-2 border-b border-slate-200">
      {enlaces.map((e) => (
        <NavLink
          key={e.ruta}
          to={e.ruta}
          className={({ isActive }) =>
            `px-3 pb-3 text-sm font-medium ${
              isActive
                ? 'border-b-2 border-impepac-magenta-600 text-impepac-magenta-600'
                : 'text-slate-500 hover:text-impepac-ink'
            }`
          }
        >
          {e.etiqueta}
        </NavLink>
      ))}
    </div>
  )
}
