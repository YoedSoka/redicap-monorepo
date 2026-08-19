package mx.gob.impepac.redicap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Estado de una subida en cola. */
object EstadoCola {
    const val PENDIENTE = "PENDIENTE"
    /** No tiene caso reintentar (el backend la rechazó por una regla de negocio, no por falta de red). */
    const val ERROR_PERMANENTE = "ERROR_PERMANENTE"
}

/**
 * Acta digitalizada en el dispositivo que aún no se confirmó subida al servidor.
 * El hash SHA-256 se calcula una sola vez, al encolar, no en cada reintento.
 */
@Entity(tableName = "actas_pendientes")
data class ActaPendiente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val casillaId: Long,
    val tipoEleccion: String,
    val rutaArchivo: String,
    val hashSha256: String,
    val creadoEn: Long = System.currentTimeMillis(),
    val intentos: Int = 0,
    val ultimoError: String? = null,
    val estado: String = EstadoCola.PENDIENTE,
)
