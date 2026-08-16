package mx.gob.impepac.redicap.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "redicap_sesion")

/** Guarda el JWT y datos básicos de sesión. Lectura síncrona vía [tokenBlocking] para el interceptor de OkHttp. */
class TokenStore(private val context: Context) {

    private val keyToken = stringPreferencesKey("token")
    private val keyUsername = stringPreferencesKey("username")
    private val keyRol = stringPreferencesKey("rol")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[keyToken] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[keyUsername] }
    val rolFlow: Flow<String?> = context.dataStore.data.map { it[keyRol] }

    suspend fun guardarSesion(token: String, username: String, rol: String) {
        context.dataStore.edit {
            it[keyToken] = token
            it[keyUsername] = username
            it[keyRol] = rol
        }
    }

    suspend fun limpiarSesion() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun haySesion(): Boolean = tokenFlow.first() != null

    /** Usado solo por el interceptor de red, que corre en un hilo de OkHttp fuera del main thread. */
    fun tokenBlocking(): String? = runBlocking { tokenFlow.first() }
}
