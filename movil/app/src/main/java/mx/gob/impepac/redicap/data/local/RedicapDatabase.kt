package mx.gob.impepac.redicap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ActaPendiente::class], version = 1, exportSchema = false)
abstract class RedicapDatabase : RoomDatabase() {
    abstract fun actaPendienteDao(): ActaPendienteDao

    companion object {
        @Volatile private var instancia: RedicapDatabase? = null

        fun obtener(context: Context): RedicapDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    RedicapDatabase::class.java,
                    "redicap.db",
                ).build().also { instancia = it }
            }
    }
}
