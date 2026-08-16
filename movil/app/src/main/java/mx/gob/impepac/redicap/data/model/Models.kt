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
)

@Serializable
data class ApiErrorBody(val status: Int, val message: String)
