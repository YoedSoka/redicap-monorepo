package mx.gob.impepac.redicap.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.local.ActaCompletada
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.data.local.SeguridadLocal
import mx.gob.impepac.redicap.data.network.extraerMensajeError
import mx.gob.impepac.redicap.data.network.subirActaBytes
import mx.gob.impepac.redicap.vision.detectarYRecortarActa
import mx.gob.impepac.redicap.worker.programarSubidaPendientes
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest

@Composable
fun CapturaScreen(
    container: AppContainer,
    casillaId: Long,
    onListo: (mensaje: String) -> Unit,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    var archivoTemporal by remember { mutableStateOf<File?>(null) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var bitmapRecortado by remember { mutableStateOf<Bitmap?>(null) }
    var legibilidadConfirmada by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val archivo = archivoTemporal
        if (!exito || archivo == null) {
            archivoTemporal = null
            fotoUri = null
            bitmapRecortado = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            procesando = true
            error = null
            try {
                val original = withContext(Dispatchers.IO) {
                    val opciones = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    BitmapFactory.decodeFile(archivo.absolutePath, opciones)
                }
                val recortado = withContext(Dispatchers.Default) { detectarYRecortarActa(original) }
                if (recortado !== original) original.recycle()
                bitmapRecortado = recortado
                legibilidadConfirmada = false
            } catch (e: Exception) {
                error = "No se pudo procesar la foto: ${e.message}"
            } finally {
                procesando = false
            }
        }
    }

    fun iniciarCaptura() {
        val carpeta = File(context.filesDir, "actas").apply { mkdirs() }
        val archivo = File(carpeta, "acta_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        archivoTemporal = archivo
        fotoUri = uri
        bitmapRecortado = null
        legibilidadConfirmada = false
        error = null
        tomarFoto.launch(uri)
    }

    fun confirmarYEnviar() {
        val bitmap = bitmapRecortado ?: return
        error = null
        guardando = true
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) {
                    ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
                }
                val hash = withContext(Dispatchers.Default) { sha256Hex(bytes) }

                val carpetaCifrada = File(context.filesDir, "actas_cifradas").apply { mkdirs() }
                val archivoCifrado = File(carpetaCifrada, "acta_${System.currentTimeMillis()}.enc")
                withContext(Dispatchers.IO) {
                    SeguridadLocal.escribirCifrado(context, archivoCifrado, bytes)
                }
                // El JPEG temporal sin cifrar que dejó la cámara ya no hace falta.
                archivoTemporal?.delete()

                try {
                    val respuesta = subirActaBytes(container.api, casillaId, hash, bytes, archivoCifrado.name)
                    archivoCifrado.delete()
                    container.database.actaCompletadaDao().insertar(
                        ActaCompletada(casillaId = casillaId, folio = respuesta.folio ?: "—")
                    )
                    onListo("Acta recibida. Folio ${respuesta.folio ?: "sin folio"}.")
                } catch (e: HttpException) {
                    if (e.code() in 500..599) {
                        encolarParaReintento(container, context, casillaId, archivoCifrado.absolutePath, hash)
                        onListo("El servidor no respondió. El acta quedó guardada y se reintentará sola.")
                    } else {
                        // Rechazo de negocio (casilla inactiva, ya digitalizada, etc.): no tiene
                        // caso encolar, el usuario debe corregir y volver a intentar.
                        archivoCifrado.delete()
                        error = extraerMensajeError(e)
                        guardando = false
                    }
                } catch (e: IOException) {
                    encolarParaReintento(container, context, casillaId, archivoCifrado.absolutePath, hash)
                    onListo("Sin conexión. El acta quedó guardada cifrada y se subirá sola.")
                }
            } catch (e: Exception) {
                error = "No se pudo guardar el acta: ${e.message}"
                guardando = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Casilla $casillaId", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                procesando -> CircularProgressIndicator()
                bitmapRecortado != null -> Image(
                    bitmap = bitmapRecortado!!.asImageBitmap(),
                    contentDescription = "Acta recortada",
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GuiaFotografiaActa()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Recuerda: los 4 puntos negros de las esquinas deben salir completos en la foto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { iniciarCaptura() }) {
                        Text("Tomar foto del acta")
                    }
                }
            }
        }

        if (bitmapRecortado != null && !procesando && !guardando) {
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
            "Si no hay conexión, el acta se guarda cifrada en el dispositivo y se sube sola en cuanto vuelva la señal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (!legibilidadConfirmada) {
            Button(
                onClick = { legibilidadConfirmada = true },
                enabled = bitmapRecortado != null && !procesando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirmar Legibilidad")
            }
        } else {
            Button(
                onClick = { confirmarYEnviar() },
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (guardando) "Guardando…" else "Enviar acta")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth(), enabled = !guardando) {
            Text("Cancelar")
        }
    }
}

/**
 * Boceto de una hoja de acta con un punto negro marcado en cada esquina — así se ve en el
 * documento real y así debe verse en la foto. Se dibuja a mano (sin asset) porque es más
 * fácil de mantener consistente con el resto de la UI que empacar un PNG.
 */
@Composable
private fun GuiaFotografiaActa() {
    Canvas(modifier = Modifier.size(150.dp, 190.dp)) {
        val margen = 14.dp.toPx()
        val radioPunto = 5.dp.toPx()
        val ancho = size.width - margen * 2
        val alto = size.height - margen * 2

        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(margen, margen),
            size = Size(ancho, alto),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )

        listOf(0.28f, 0.40f, 0.52f, 0.64f).forEach { fraccionY ->
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(margen + ancho * 0.18f, margen + alto * fraccionY),
                end = Offset(margen + ancho * 0.82f, margen + alto * fraccionY),
                strokeWidth = 3.dp.toPx(),
            )
        }

        listOf(
            Offset(margen, margen),
            Offset(margen + ancho, margen),
            Offset(margen, margen + alto),
            Offset(margen + ancho, margen + alto),
        ).forEach { esquina ->
            drawCircle(color = Color.Black, radius = radioPunto, center = esquina)
        }
    }
}

private suspend fun encolarParaReintento(
    container: AppContainer,
    context: android.content.Context,
    casillaId: Long,
    rutaArchivoCifrado: String,
    hashSha256: String,
) {
    container.database.actaPendienteDao().insertar(
        ActaPendiente(casillaId = casillaId, rutaArchivo = rutaArchivoCifrado, hashSha256 = hashSha256)
    )
    programarSubidaPendientes(context)
}

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
