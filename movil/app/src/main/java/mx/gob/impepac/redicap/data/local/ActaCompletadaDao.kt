package mx.gob.impepac.redicap.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActaCompletadaDao {

    @Insert
    suspend fun insertar(acta: ActaCompletada): Long

    @Query("SELECT * FROM actas_completadas ORDER BY completadaEn DESC")
    fun observarTodas(): Flow<List<ActaCompletada>>

    @Delete
    suspend fun eliminar(acta: ActaCompletada)
}
