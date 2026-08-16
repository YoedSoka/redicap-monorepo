package mx.gob.impepac.redicap.data.network

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
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
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

/** Traduce errores de red/HTTP al mensaje en español que ya manda el backend. */
suspend fun <T> llamar(bloque: suspend () -> T): Result<T> {
    return try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val mensaje = try {
            val body = e.response()?.errorBody()?.string()
            body?.let { json.decodeFromString<ApiErrorBody>(it).message }
        } catch (parseError: Exception) {
            null
        } ?: "Ocurrió un error inesperado (${e.code()})"
        Result.failure(RuntimeException(mensaje))
    } catch (e: IOException) {
        Result.failure(RuntimeException("No se pudo conectar con el servidor. Revisa tu conexión."))
    } catch (e: Exception) {
        Result.failure(RuntimeException(e.message ?: "Ocurrió un error inesperado"))
    }
}
