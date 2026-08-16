package mx.gob.impepac.redicap.data.network

import mx.gob.impepac.redicap.data.model.ActaResponse
import mx.gob.impepac.redicap.data.model.CasillaResponse
import mx.gob.impepac.redicap.data.model.DistritoResponse
import mx.gob.impepac.redicap.data.model.LoginRequest
import mx.gob.impepac.redicap.data.model.MunicipioResponse
import mx.gob.impepac.redicap.data.model.SeccionResponse
import mx.gob.impepac.redicap.data.model.TokenResponse
import mx.gob.impepac.redicap.data.model.UsuarioResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("usuarios/me")
    suspend fun obtenerPerfil(): UsuarioResponse

    @Multipart
    @POST("digitalizacion")
    suspend fun subirActa(
        @Part("casillaId") casillaId: RequestBody,
        @Part("hashSha256") hashSha256: RequestBody,
        @Part imagen: MultipartBody.Part,
    ): ActaResponse

    @GET("distritos")
    suspend fun listarDistritos(): List<DistritoResponse>

    @GET("municipios")
    suspend fun listarMunicipios(): List<MunicipioResponse>

    @GET("secciones")
    suspend fun listarSecciones(@Query("municipioId") municipioId: Long): List<SeccionResponse>

    @GET("casillas")
    suspend fun listarCasillas(@Query("seccionId") seccionId: Long): List<CasillaResponse>

    @GET("casillas/{id}")
    suspend fun obtenerCasilla(@Path("id") id: Long): CasillaResponse
}
