package mx.gob.impepac.redicap.worker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.local.ActaCompletada
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.data.local.EstadoCola
import mx.gob.impepac.redicap.data.local.SeguridadLocal
import mx.gob.impepac.redicap.data.network.extraerMensajeError
import mx.gob.impepac.redicap.data.network.subirActaBytes
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TRABAJO_SUBIDA = "subida-actas-pendientes"

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
            val bytes = SeguridadLocal.leerCifrado(applicationContext, archivo)
            val respuesta = subirActaBytes(
                container.api, pendiente.casillaId, pendiente.tipoEleccion, pendiente.hashSha256, bytes, archivo.name
            )

            dao.eliminar(pendiente)
            archivo.delete()
            container.database.actaCompletadaDao().insertar(
                ActaCompletada(casillaId = pendiente.casillaId, folio = respuesta.folio ?: "—")
            )
            true
        } catch (e: HttpException) {
            if (e.code() in 500..599 || e.code() == 401) {
                // Servidor caído o sesión vencida (JWT expira a las 8h, una jornada dura más):
                // ambos se arreglan solos, uno cuando el servidor vuelve, el otro cuando el
                // usuario vuelve a entrar — el interceptor de red ya lo mandó a login.
                val motivo = if (e.code() == 401) "Sesión vencida" else "Error del servidor (${e.code()})"
                dao.registrarIntentoFallido(pendiente.id, motivo)
                false
            } else {
                // Rechazo de negocio (hash no coincide, casilla inactiva, ya digitalizada, etc.):
                // reintentar no lo va a arreglar. Se marca para que el usuario decida.
                dao.marcarEstado(pendiente.id, EstadoCola.ERROR_PERMANENTE, extraerMensajeError(e))
                true
            }
        } catch (e: IOException) {
            // Sin conexión o se cortó la subida a medias: reintentar cuando vuelva la red.
            dao.registrarIntentoFallido(pendiente.id, "Sin conexión")
            false
        }
    }
}
