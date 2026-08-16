package mx.gob.impepac.redicap.data

import android.content.Context
import mx.gob.impepac.redicap.data.local.RedicapDatabase
import mx.gob.impepac.redicap.data.network.ApiClient
import mx.gob.impepac.redicap.data.network.ApiService
import mx.gob.impepac.redicap.data.network.TokenStore

/** Contenedor manual de dependencias (sin Hilt, para mantener el proyecto simple). */
class AppContainer(context: Context) {
    val tokenStore = TokenStore(context.applicationContext)
    val api: ApiService = ApiClient.crear(tokenStore)
    val database = RedicapDatabase.obtener(context.applicationContext)
}
