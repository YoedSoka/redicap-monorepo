package mx.gob.impepac.redicap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [ActaPendiente::class, ActaCompletada::class], version = 3, exportSchema = false)
abstract class RedicapDatabase : RoomDatabase() {
    abstract fun actaPendienteDao(): ActaPendienteDao
    abstract fun actaCompletadaDao(): ActaCompletadaDao

    companion object {
        @Volatile private var instancia: RedicapDatabase? = null
        @Volatile private var libCargada = false

        fun obtener(context: Context): RedicapDatabase =
            instancia ?: synchronized(this) {
                instancia ?: run {
                    if (!libCargada) {
                        System.loadLibrary("sqlcipher")
                        libCargada = true
                    }
                    val passphrase = SeguridadLocal.obtenerPassphrase(context.applicationContext)
                    Room.databaseBuilder(
                        context.applicationContext,
                        RedicapDatabase::class.java,
                        "redicap.db",
                    )
                        .openHelperFactory(SupportOpenHelperFactory(passphrase))
                        // No hay datos de producción todavía en esta app; simplifica el
                        // versionado del esquema mientras el modelo local sigue cambiando.
                        .fallbackToDestructiveMigration()
                        .build()
                }.also { instancia = it }
            }
    }
}
