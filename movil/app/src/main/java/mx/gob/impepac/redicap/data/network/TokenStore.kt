package mx.gob.impepac.redicap.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

    /** Emite cuando el interceptor detecta una sesión muerta (token invalidado por sesión
     * única en otro dispositivo, o expirado). buffer=1 para no perder el evento si la UI
     * todavía no está observando cuando ocurre. */
    private val _sesionInvalidada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionInvalidada: SharedFlow<Unit> = _sesionInvalidada

    /**
     * Llamado solo por el interceptor de red al recibir 401 en una petición autenticada,
     * desde dentro de un runBlocking en el hilo del dispatcher de OkHttp. tryEmit (no emit)
     * a propósito: con extraBufferCapacity=1 y sin collector activo (app en background, o el
     * worker corriendo sin UI), un segundo evento antes de que alguien lea el primero
     * suspendería para siempre y colgaría ese hilo de OkHttp. Perder un duplicado no importa,
     * el primero ya basta para mandar a login.
     */
    suspend fun notificarSesionInvalidada() {
        limpiarSesion()
        _sesionInvalidada.tryEmit(Unit)
    }

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
