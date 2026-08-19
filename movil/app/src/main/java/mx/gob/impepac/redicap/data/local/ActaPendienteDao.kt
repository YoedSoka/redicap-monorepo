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

    /** "Reintentar ahora" sobre un acta en ERROR_PERMANENTE: sin esto, listarPendientes()
     * (que solo trae estado = 'PENDIENTE') nunca la vuelve a considerar aunque se reprograme
     * el worker — el botón se vería activo pero no haría nada. */
    @Query("UPDATE actas_pendientes SET estado = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarPendiente(id: Long)

    @Query("UPDATE actas_pendientes SET estado = 'PENDIENTE' WHERE estado = 'ERROR_PERMANENTE'")
    suspend fun marcarTodosPendientes()
}
