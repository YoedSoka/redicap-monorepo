package mx.gob.impepac.redicap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Confirmación de recepción del servidor (DFR R1: folio + indicador visual) para un acta ya
 * subida, sea de forma síncrona o tras un reintento del worker. Se guarda hasta que el usuario
 * la descarta en HomeScreen; no representa nada pendiente de subir.
 */
@Entity(tableName = "actas_completadas")
data class ActaCompletada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val casillaId: Long,
    val folio: String,
    val completadaEn: Long = System.currentTimeMillis(),
)
