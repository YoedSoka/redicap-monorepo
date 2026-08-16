import axios from 'axios'

export interface TokenResponse {
  token: string
  username: string
  rol: string
  expiresInMs: number
}

export interface ApiError {
  status: number
  message: string
}

export type EstadoActa =
  | 'RECIBIDA'
  | 'EN_CAPTURA_1'
  | 'EN_CAPTURA_2'
  | 'EN_CAPTURA_3'
  | 'MESA_DELIBERACION'
  | 'VALIDADA'
  | 'VALIDADA_VERIFICADOR'
  | 'ILEGIBLE'
  | 'PUBLICADA'

export interface ActaResponse {
  id: number
  casillaId: number
  estado: EstadoActa
  rutaImagen: string | null
  errorAritmetico: boolean
  excedeListaNominal: boolean
}

export interface CapturaResumenResponse {
  numeroCaptura: number
  capturistaId: number
  votos: Record<string, number>
  totalVotosActa: number | null
  totalVotosCalculado: number
}

export interface VerificacionDetalleResponse {
  acta: ActaResponse
  capturas: CapturaResumenResponse[]
}

export type Rol = 'ADMINISTRADOR' | 'DIGITALIZADOR' | 'CAPTURISTA' | 'VERIFICADOR' | 'CONSULTOR_PUBLICO'

export interface UsuarioResponse {
  id: number
  username: string
  nombreCompleto: string
  curp: string | null
  rol: Rol
  casillaAsignadaId: number | null
  activo: boolean
  intentosFallidos: number
  bloqueadoHasta: string | null
  createdAt: string
}

export interface CrearUsuarioRequest {
  username: string
  password: string
  nombreCompleto: string
  curp?: string
  rol: Rol
  casillaAsignadaId?: number
}

export interface ActualizarUsuarioRequest {
  nombreCompleto: string
  curp?: string
  rol: Rol
  casillaAsignadaId?: number
}

export interface CasillaResponse {
  id: number
  numeroSeccion: number
  tipo: string
  numeroCasilla: number
}

export interface CorteResponse {
  id: number
  generadoAt: string
  totalActasCapturadas: number
  totalActasValidadas: number
  totalCasillas: number
  porcentajeParticipacion: number | null
  resultados: Record<string, number>
}

const api = axios.create({
  baseURL: '/api/v1',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('redicap.token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Autenticación ────────────────────────────────────────────────────────

export async function login(username: string, password: string): Promise<TokenResponse> {
  const { data } = await api.post<TokenResponse>('/auth/login', { username, password })
  return data
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout')
}

// ── Captura (doble ciego) ────────────────────────────────────────────────

export async function obtenerSiguienteActa(): Promise<ActaResponse | null> {
  try {
    const { data } = await api.get<ActaResponse>('/capturas/siguiente')
    return data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 204) {
      return null
    }
    throw error
  }
}

export async function registrarCaptura(
  actaId: number,
  votos: Record<string, number>,
  totalVotosActa?: number,
): Promise<ActaResponse> {
  const { data } = await api.post<ActaResponse>(`/capturas/${actaId}`, { votos, totalVotosActa })
  return data
}

// ── Digitalización ───────────────────────────────────────────────────────

export async function calcularSha256(archivo: File): Promise<string> {
  const buffer = await archivo.arrayBuffer()
  const hash = await crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(hash))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

export async function subirActaDigitalizada(casillaId: number, archivo: File): Promise<ActaResponse> {
  const hashSha256 = await calcularSha256(archivo)
  const form = new FormData()
  form.append('casillaId', String(casillaId))
  form.append('hashSha256', hashSha256)
  form.append('imagen', archivo)
  const { data } = await api.post<ActaResponse>('/digitalizacion', form)
  return data
}

// ── Verificación (mesa de deliberación) ─────────────────────────────────

export async function listarPendientesVerificacion(): Promise<ActaResponse[]> {
  const { data } = await api.get<ActaResponse[]>('/verificaciones/pendientes')
  return data
}

export async function obtenerDetalleVerificacion(actaId: number): Promise<VerificacionDetalleResponse> {
  const { data } = await api.get<VerificacionDetalleResponse>(`/verificaciones/${actaId}`)
  return data
}

export async function validarVerificacion(actaId: number, numeroCapturaElegida: number): Promise<ActaResponse> {
  const { data } = await api.post<ActaResponse>(`/verificaciones/${actaId}/validar`, { numeroCapturaElegida })
  return data
}

export async function marcarIlegible(actaId: number, motivo: string): Promise<ActaResponse> {
  const { data } = await api.post<ActaResponse>(`/verificaciones/${actaId}/ilegible`, { motivo })
  return data
}

// ── Usuarios (administración) ────────────────────────────────────────────

export async function listarUsuarios(): Promise<UsuarioResponse[]> {
  const { data } = await api.get<UsuarioResponse[]>('/usuarios')
  return data
}

export async function crearUsuario(request: CrearUsuarioRequest): Promise<UsuarioResponse> {
  const { data } = await api.post<UsuarioResponse>('/usuarios', request)
  return data
}

export async function actualizarUsuario(id: number, request: ActualizarUsuarioRequest): Promise<UsuarioResponse> {
  const { data } = await api.put<UsuarioResponse>(`/usuarios/${id}`, request)
  return data
}

export async function desbloquearUsuario(id: number): Promise<UsuarioResponse> {
  const { data } = await api.post<UsuarioResponse>(`/usuarios/${id}/desbloquear`)
  return data
}

export async function cambiarActivoUsuario(id: number, activo: boolean): Promise<UsuarioResponse> {
  const { data } = await api.post<UsuarioResponse>(`/usuarios/${id}/${activo ? 'activar' : 'desactivar'}`)
  return data
}

export async function listarCasillas(): Promise<CasillaResponse[]> {
  const { data } = await api.get<CasillaResponse[]>('/casillas')
  return data
}

// ── Publicación (consulta pública) ───────────────────────────────────────

export async function obtenerUltimoCorte(): Promise<CorteResponse | null> {
  try {
    const { data } = await api.get<CorteResponse>('/publicacion/ultimo-corte')
    return data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 204) {
      return null
    }
    throw error
  }
}

export async function obtenerHistorialCortes(): Promise<CorteResponse[]> {
  const { data } = await api.get<CorteResponse[]>('/publicacion/historial')
  return data
}

// ── Utilidades ───────────────────────────────────────────────────────────

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return 'No se pudo conectar con el servidor. Intenta de nuevo.'
}

export default api
