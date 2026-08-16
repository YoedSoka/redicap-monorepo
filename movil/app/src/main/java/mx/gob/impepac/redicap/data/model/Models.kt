package mx.gob.impepac.redicap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(
    val token: String,
    val username: String,
    val rol: String,
    val expiresInMs: Long,
)

@Serializable
data class UsuarioResponse(
    val id: Long,
    val username: String,
    val nombreCompleto: String,
    val curp: String? = null,
    val rol: String,
    val casillaAsignadaId: Long? = null,
    val activo: Boolean,
)

@Serializable
data class ActaResponse(
    val id: Long,
    val casillaId: Long,
    val estado: String,
    val rutaImagen: String? = null,
    val errorAritmetico: Boolean = false,
    val excedeListaNominal: Boolean = false,
    val folio: String? = null,
)

@Serializable
data class DistritoResponse(
    val id: Long,
    val clave: String,
    val nombre: String,
    val cabeceraDistrital: String? = null,
)

@Serializable
data class MunicipioResponse(
    val id: Long,
    val clave: String,
    val nombre: String,
)

@Serializable
data class SeccionResponse(
    val id: Long,
    val numeroSeccion: Int,
    val municipioId: Long,
    val municipioNombre: String,
    val distritoId: Long,
    val distritoNombre: String,
)

@Serializable
data class CasillaResponse(
    val id: Long,
    val seccionId: Long,
    val numeroSeccion: Int,
    val tipo: String,
    val numeroCasilla: Int,
    val listaNominal: Int,
    val activa: Boolean,
    val municipioNombre: String,
    val distritoNombre: String,
)

@Serializable
data class ApiErrorBody(val status: Int, val message: String)
