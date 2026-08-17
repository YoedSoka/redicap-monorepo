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

export type TipoCasilla = 'BASICA' | 'CONTIGUA' | 'ESPECIAL' | 'EXTRAORDINARIA'

export interface DistritoResponse {
  id: number
  clave: string
  nombre: string
  cabeceraDistrital: string | null
}

export interface CrearDistritoRequest {
  clave: string
  nombre: string
  cabeceraDistrital?: string
}

export interface MunicipioResponse {
  id: number
  clave: string
  nombre: string
}

export interface CrearMunicipioRequest {
  clave: string
  nombre: string
}

export interface SeccionResponse {
  id: number
  numeroSeccion: number
  municipioId: number
  municipioNombre: string
  distritoId: number
  distritoNombre: string
}

export interface CrearSeccionRequest {
  numeroSeccion: number
  municipioId: number
  distritoId: number
}

export interface CasillaResponse {
  id: number
  seccionId: number
  numeroSeccion: number
  tipo: TipoCasilla
  numeroCasilla: number
  listaNominal: number
  activa: boolean
  municipioNombre: string
  distritoNombre: string
}

export interface CrearCasillaRequest {
  seccionId: number
  tipo: TipoCasilla
  numeroCasilla: number
  listaNominal: number
}

export interface PartidoPoliticoResponse {
  id: number
  siglas: string
  nombre: string
  colorHex: string | null
  activo: boolean
}

export interface CrearPartidoRequest {
  siglas: string
  nombre: string
  colorHex?: string
}

export interface ActualizarPartidoRequest {
  nombre: string
  colorHex?: string
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

/** Devuelve una object URL con la imagen del acta; hay que revocarla (URL.revokeObjectURL) cuando ya no se use. */
export async function obtenerImagenActaUrl(actaId: number): Promise<string> {
  const { data } = await api.get(`/capturas/${actaId}/imagen`, { responseType: 'blob' })
  return URL.createObjectURL(data)
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

// ── Catálogo geográfico (administración) ─────────────────────────────────

export async function listarDistritos(): Promise<DistritoResponse[]> {
  const { data } = await api.get<DistritoResponse[]>('/distritos')
  return data
}

export async function crearDistrito(request: CrearDistritoRequest): Promise<DistritoResponse> {
  const { data } = await api.post<DistritoResponse>('/distritos', request)
  return data
}

export async function eliminarDistrito(id: number): Promise<void> {
  await api.delete(`/distritos/${id}`)
}

export async function listarMunicipios(): Promise<MunicipioResponse[]> {
  const { data } = await api.get<MunicipioResponse[]>('/municipios')
  return data
}

export async function crearMunicipio(request: CrearMunicipioRequest): Promise<MunicipioResponse> {
  const { data } = await api.post<MunicipioResponse>('/municipios', request)
  return data
}

export async function eliminarMunicipio(id: number): Promise<void> {
  await api.delete(`/municipios/${id}`)
}

export async function listarSecciones(): Promise<SeccionResponse[]> {
  const { data } = await api.get<SeccionResponse[]>('/secciones')
  return data
}

export async function crearSeccion(request: CrearSeccionRequest): Promise<SeccionResponse> {
  const { data } = await api.post<SeccionResponse>('/secciones', request)
  return data
}

export async function eliminarSeccion(id: number): Promise<void> {
  await api.delete(`/secciones/${id}`)
}

export async function crearCasilla(request: CrearCasillaRequest): Promise<CasillaResponse> {
  const { data } = await api.post<CasillaResponse>('/casillas', request)
  return data
}

export async function eliminarCasilla(id: number): Promise<void> {
  await api.delete(`/casillas/${id}`)
}

// ── Partidos políticos ────────────────────────────────────────────────────

export async function listarPartidos(): Promise<PartidoPoliticoResponse[]> {
  const { data } = await api.get<PartidoPoliticoResponse[]>('/partidos')
  return data
}

export async function crearPartido(request: CrearPartidoRequest): Promise<PartidoPoliticoResponse> {
  const { data } = await api.post<PartidoPoliticoResponse>('/partidos', request)
  return data
}

export async function actualizarPartido(id: number, request: ActualizarPartidoRequest): Promise<PartidoPoliticoResponse> {
  const { data } = await api.put<PartidoPoliticoResponse>(`/partidos/${id}`, request)
  return data
}

export async function cambiarActivoPartido(id: number, activo: boolean): Promise<PartidoPoliticoResponse> {
  const { data } = await api.post<PartidoPoliticoResponse>(`/partidos/${id}/${activo ? 'activar' : 'desactivar'}`)
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
