export const ROLES = ['ADMINISTRADOR', 'DIGITALIZADOR', 'CAPTURISTA', 'VERIFICADOR', 'CONSULTOR_PUBLICO'] as const

export const NOMBRE_ROL: Record<string, string> = {
  ADMINISTRADOR: 'Administrador',
  DIGITALIZADOR: 'Digitalizador',
  CAPTURISTA: 'Capturista',
  VERIFICADOR: 'Verificador',
  CONSULTOR_PUBLICO: 'Consultor público',
}

export function rutaPorRol(rol: string): string {
  switch (rol) {
    case 'CAPTURISTA':
      return '/captura'
    case 'DIGITALIZADOR':
      return '/digitalizacion'
    case 'VERIFICADOR':
      return '/verificacion'
    case 'ADMINISTRADOR':
      return '/admin/usuarios'
    case 'CONSULTOR_PUBLICO':
      return '/consulta'
    default:
      return '/'
  }
}
