package mx.gob.impepac.redicap

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.ui.theme.RedicapTheme
import mx.gob.impepac.redicap.vision.opencvReady
import mx.gob.impepac.redicap.worker.programarSubidaPendientes
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicialización síncrona (la AAR moderna de Maven Central trae los .so incluidos,
        // no requiere la app externa "OpenCV Manager" de versiones antiguas). Si falla,
        // opencvReady queda en false y RecortadorActa cae siempre al fallback sin recorte.
        opencvReady = try {
            OpenCVLoader.initLocal()
        } catch (e: Exception) {
            Log.e("MainActivity", "No se pudo inicializar OpenCV", e)
            false
        }

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
