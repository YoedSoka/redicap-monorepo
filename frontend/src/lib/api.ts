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

/** Cada casilla produce hasta 3 actas independientes, una por elección (DFR R4/R5/R6). */
export type TipoEleccion = 'GUBERNATURA' | 'DIPUTACION_LOCAL' | 'AYUNTAMIENTO'

export const ELECCIONES: TipoEleccion[] = ['GUBERNATURA', 'DIPUTACION_LOCAL', 'AYUNTAMIENTO']

export const ETIQUETAS_ELECCION: Record<TipoEleccion, string> = {
  GUBERNATURA: 'Gubernatura',
  DIPUTACION_LOCAL: 'Diputación Local',
  AYUNTAMIENTO: 'Ayuntamiento',
}

export interface ActaResponse {
  id: number
  casillaId: number
  tipoEleccion: TipoEleccion
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

/** Catálogo de motivos para toda resolución del Verificador (DFR R3: justificación obligatoria). */
export type MotivoDictamenVerificador =
  | 'COINCIDENCIA_CLARA_CON_ACTA_FISICA'
  | 'ERROR_DE_CAPTURA_EVIDENTE'
  | 'IMAGEN_BORROSA_O_MOVIDA'
  | 'OBSTRUCCION_O_DANO_FISICO'
  | 'ILUMINACION_INSUFICIENTE'
  | 'OTRO'

export const ETIQUETAS_MOTIVO_DICTAMEN: Record<MotivoDictamenVerificador, string> = {
  COINCIDENCIA_CLARA_CON_ACTA_FISICA: 'Coincidencia clara con el acta física',
  ERROR_DE_CAPTURA_EVIDENTE: 'Error de captura evidente en las otras capturas',
  IMAGEN_BORROSA_O_MOVIDA: 'Imagen borrosa o movida',
  OBSTRUCCION_O_DANO_FISICO: 'Obstrucción o daño físico en el papel',
  ILUMINACION_INSUFICIENTE: 'Iluminación insuficiente',
  OTRO: 'Otro',
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
  tipoEleccion: TipoEleccion
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
  // axios no lanza para 204 (es 2xx): sin acta pendiente, el body viene vacío, no null.
  const { data, status } = await api.get<ActaResponse>('/capturas/siguiente')
  return status === 204 ? null : data
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
// No hay función web para subir actas: la digitalización (DFR R1) es cámara
// obligatoria en tiempo real, exclusiva de la app Android. POST /digitalizacion
// solo lo consume movil/ (ver ApiService.kt).

// ── Verificación (mesa de deliberación) ─────────────────────────────────

export async function listarPendientesVerificacion(): Promise<ActaResponse[]> {
  const { data } = await api.get<ActaResponse[]>('/verificaciones/pendientes')
  return data
}

export async function obtenerDetalleVerificacion(actaId: number): Promise<VerificacionDetalleResponse> {
  const { data } = await api.get<VerificacionDetalleResponse>(`/verificaciones/${actaId}`)
  return data
}

export async function validarVerificacion(
  actaId: number,
  numeroCapturaElegida: number,
  motivoCatalogo: MotivoDictamenVerificador,
  justificacion: string,
): Promise<ActaResponse> {
  const { data } = await api.post<ActaResponse>(`/verificaciones/${actaId}/validar`, {
    numeroCapturaElegida,
    motivoCatalogo,
    justificacion,
  })
  return data
}

export async function marcarIlegible(
  actaId: number,
  motivoCatalogo: MotivoDictamenVerificador,
  justificacion: string,
): Promise<ActaResponse> {
  const { data } = await api.post<ActaResponse>(`/verificaciones/${actaId}/ilegible`, {
    motivoCatalogo,
    justificacion,
  })
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

export async function obtenerUltimoCorte(tipoEleccion: TipoEleccion): Promise<CorteResponse | null> {
  // axios no lanza para 204 (es 2xx): sin corte publicado, el body viene vacío, no null.
  const { data, status } = await api.get<CorteResponse>(`/publicacion/${tipoEleccion}/ultimo-corte`)
  return status === 204 ? null : data
}

export async function obtenerHistorialCortes(tipoEleccion: TipoEleccion): Promise<CorteResponse[]> {
  const { data } = await api.get<CorteResponse[]>(`/publicacion/${tipoEleccion}/historial`)
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
