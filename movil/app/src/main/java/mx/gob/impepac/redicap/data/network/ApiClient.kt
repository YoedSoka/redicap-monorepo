package mx.gob.impepac.redicap.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mx.gob.impepac.redicap.BuildConfig
import mx.gob.impepac.redicap.data.model.ApiErrorBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private val json = Json { ignoreUnknownKeys = true }

object ApiClient {

    fun crear(tokenStore: TokenStore): ApiService {
        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = tokenStore.tokenBlocking()
            val builder = chain.request().newBuilder()
                // ngrok antepone una página de advertencia al tráfico HTML de navegador en su
                // plan gratuito. Este header la salta; fuera de ngrok cualquier otro servidor
                // simplemente ignora un header que no conoce.
                .addHeader("ngrok-skip-browser-warning", "1")
            if (token != null) {
                builder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        // Si una petición que sí llevaba token recibe 401, la sesión murió (invalidada por
        // "sesión única" al entrar en otro dispositivo, o expiró). Sin esto el usuario se
        // queda atorado viendo "Ocurrió un error inesperado (401)" con un token muerto.
        // No dispara con el 401 de /auth/login (credenciales incorrectas), porque esa
        // petición nunca lleva el header Authorization.
        val sesionMuertaInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code == 401 && request.header("Authorization") != null) {
                runBlocking { tokenStore.notificarSesionInvalidada() }
            }
            response
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sesionMuertaInterceptor)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

/** Extrae el mensaje en español que ya manda el backend en el cuerpo de error de una HttpException. */
fun extraerMensajeError(e: HttpException): String {
    return try {
        val body = e.response()?.errorBody()?.string()
        body?.let { json.decodeFromString<ApiErrorBody>(it).message }
    } catch (parseError: Exception) {
        null
    } ?: "Ocurrió un error inesperado (${e.code()})"
}

/** Traduce errores de red/HTTP al mensaje en español que ya manda el backend. */
suspend fun <T> llamar(bloque: suspend () -> T): Result<T> {
    return try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(RuntimeException(extraerMensajeError(e)))
    } catch (e: IOException) {
        Result.failure(RuntimeException("No se pudo conectar con el servidor. Revisa tu conexión."))
    } catch (e: Exception) {
        Result.failure(RuntimeException(e.message ?: "Ocurrió un error inesperado"))
    }
}
