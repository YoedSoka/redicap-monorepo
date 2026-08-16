package mx.gob.impepac.redicap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.ui.theme.RedicapTheme
import mx.gob.impepac.redicap.worker.programarSubidaPendientes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(applicationContext)
        // Red de seguridad: si quedó algo en la cola de una sesión anterior (ej. la app
        // se cerró antes de que WorkManager pudiera correr), esto lo vuelve a programar.
        // No hace nada si ya hay un trabajo en cola (ExistingWorkPolicy.KEEP).
        programarSubidaPendientes(applicationContext)

        setContent {
            RedicapTheme {
                RedicapApp(container = container)
            }
        }
    }
}
