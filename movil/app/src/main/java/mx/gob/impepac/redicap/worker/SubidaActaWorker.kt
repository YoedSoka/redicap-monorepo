package mx.gob.impepac.redicap.worker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.data.local.EstadoCola
import mx.gob.impepac.redicap.data.model.ApiErrorBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TRABAJO_SUBIDA = "subida-actas-pendientes"
private val json = Json { ignoreUnknownKeys = true }

/**
 * Encola el procesamiento de la cola de subidas pendientes. Solo corre cuando hay red
 * (constraint), y WorkManager la re-dispara solo si la conexión se recupera después de
 * fallar — no hace falta ningún listener de conectividad a mano.
 */
fun programarSubidaPendientes(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val solicitud = OneTimeWorkRequestBuilder<SubidaActaWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(TRABAJO_SUBIDA, ExistingWorkPolicy.KEEP, solicitud)
}

class SubidaActaWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = AppContainer(applicationContext)
        val dao = container.database.actaPendienteDao()
        val pendientes = dao.listarPendientes()

        var quedaAlgunaPendiente = false

        for (pendiente in pendientes) {
            val archivo = File(pendiente.rutaArchivo)
            if (!archivo.exists()) {
                // El archivo se perdió (ej. se limpió la caché); no hay nada que reintentar.
                dao.eliminar(pendiente)
                continue
            }

            val exito = intentarSubir(container, pendiente, archivo)
            if (!exito) {
                quedaAlgunaPendiente = true
            }
        }

        if (quedaAlgunaPendiente) Result.retry() else Result.success()
    }

    /** @return true si se subió (o se descartó definitivamente); false si hay que reintentar más tarde. */
    private suspend fun intentarSubir(container: AppContainer, pendiente: ActaPendiente, archivo: File): Boolean {
        val dao = container.database.actaPendienteDao()
        return try {
            val casillaBody = pendiente.casillaId.toString().toRequestBody("text/plain".toMediaType())
            val hashBody = pendiente.hashSha256.toRequestBody("text/plain".toMediaType())
            val imagenPart = MultipartBody.Part.createFormData(
                "imagen", archivo.name, archivo.asRequestBody("image/jpeg".toMediaType())
            )
            container.api.subirActa(casillaBody, hashBody, imagenPart)

            dao.eliminar(pendiente)
            archivo.delete()
            true
        } catch (e: HttpException) {
            if (e.code() in 500..599) {
                // Falla del servidor: vale la pena reintentar.
                dao.registrarIntentoFallido(pendiente.id, "Error del servidor (${e.code()})")
                false
            } else {
                // Rechazo de negocio (hash no coincide, casilla no asignada, ya digitalizada, etc.):
                // reintentar no lo va a arreglar. Se marca para que el usuario decida.
                val mensaje = mensajeDeError(e)
                dao.marcarEstado(pendiente.id, EstadoCola.ERROR_PERMANENTE, mensaje)
                true
            }
        } catch (e: IOException) {
            // Sin conexión o se cortó la subida a medias: reintentar cuando vuelva la red.
            dao.registrarIntentoFallido(pendiente.id, "Sin conexión")
            false
        }
    }

    private fun mensajeDeError(e: HttpException): String {
        return try {
            val body = e.response()?.errorBody()?.string()
            body?.let { json.decodeFromString<ApiErrorBody>(it).message } ?: "Error ${e.code()}"
        } catch (ex: Exception) {
            "Error ${e.code()}"
        }
    }
}
