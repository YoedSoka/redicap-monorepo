package mx.gob.impepac.redicap.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActaPendienteDao {

    @Insert
    suspend fun insertar(acta: ActaPendiente): Long

    @Query("SELECT * FROM actas_pendientes WHERE estado = 'PENDIENTE' ORDER BY creadoEn ASC")
    suspend fun listarPendientes(): List<ActaPendiente>

    @Query("SELECT * FROM actas_pendientes ORDER BY creadoEn ASC")
    fun observarTodas(): Flow<List<ActaPendiente>>

    @Delete
    suspend fun eliminar(acta: ActaPendiente)

    @Query("UPDATE actas_pendientes SET intentos = intentos + 1, ultimoError = :error WHERE id = :id")
    suspend fun registrarIntentoFallido(id: Long, error: String?)

    @Query("UPDATE actas_pendientes SET estado = :estado, ultimoError = :error WHERE id = :id")
    suspend fun marcarEstado(id: Long, estado: String, error: String?)
}
