package mx.gob.impepac.redicap.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.worker.programarSubidaPendientes
import java.io.File
import java.security.MessageDigest

@Composable
fun CapturaScreen(
    container: AppContainer,
    casillaId: Long,
    onListo: (mensaje: String) -> Unit,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    var archivoActa by remember { mutableStateOf<File?>(null) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    // Estado aparte de archivoActa/fotoUri: la cámara escribe el archivo en disco DESPUÉS
    // de que ya seteamos esas dos variables (para poder pasarle el Uri), así que
    // `archivoActa.exists()` evaluado en la composición inicial siempre da falso.
    // Sin este flag explícito, Compose no tiene motivo para recomponer cuando la
    // cámara termina con éxito (ninguna otra State cambia), y el botón "Tomar foto"
    // se queda mostrado para siempre aunque la foto ya se haya tomado.
    var fotoLista by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) {
            fotoLista = true
        } else {
            archivoActa = null
            fotoUri = null
            fotoLista = false
        }
    }

    fun iniciarCaptura() {
        // filesDir (no cacheDir): la foto debe sobrevivir hasta que se confirme la subida
        // al servidor, y el sistema puede limpiar la caché sin avisar en cualquier momento.
        val carpeta = File(context.filesDir, "actas").apply { mkdirs() }
        val archivo = File(carpeta, "acta_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        archivoActa = archivo
        fotoUri = uri
        fotoLista = false
        tomarFoto.launch(uri)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Casilla $casillaId", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (fotoLista && fotoUri != null) {
                AsyncImage(
                    model = fotoUri,
                    contentDescription = "Foto del acta",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                OutlinedButton(onClick = { iniciarCaptura() }) {
                    Text("Tomar foto del acta")
                }
            }
        }

        if (fotoUri != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { iniciarCaptura() }) {
                Text("Tomar de nuevo")
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Si no hay conexión, el acta se guarda en el dispositivo y se sube sola en cuanto vuelva la señal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val archivo = archivoActa ?: return@Button
                error = null
                guardando = true
                scope.launch {
                    try {
                        val hash = withContext(Dispatchers.IO) { sha256Hex(archivo) }
                        container.database.actaPendienteDao().insertar(
                            ActaPendiente(casillaId = casillaId, rutaArchivo = archivo.absolutePath, hashSha256 = hash)
                        )
                        programarSubidaPendientes(context)
                        onListo("Acta guardada. Se subirá en cuanto haya conexión.")
                    } catch (e: Exception) {
                        guardando = false
                        error = "No se pudo guardar el acta: ${e.message}"
                    }
                }
            },
            enabled = archivoActa != null && !guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (guardando) "Guardando…" else "Subir acta")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }
}

private fun sha256Hex(archivo: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    archivo.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var leidos: Int
        while (input.read(buffer).also { leidos = it } != -1) {
            digest.update(buffer, 0, leidos)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
