package mx.gob.impepac.redicap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.local.ActaCompletada
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.data.local.EstadoCola
import mx.gob.impepac.redicap.ui.theme.ImpepacMagenta50
import mx.gob.impepac.redicap.ui.theme.ImpepacMagenta600
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple300
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple500
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple900
import mx.gob.impepac.redicap.worker.programarSubidaPendientes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoHora = SimpleDateFormat("d MMM, HH:mm", Locale("es", "MX"))

/**
 * Pantalla dedicada a la cola de envíos: qué actas quedaron pendientes por falta de conexión
 * (o rechazo temporal del servidor) y cuáles ya se confirmaron. HomeScreen solo muestra un
 * resumen y trae aquí para el detalle completo.
 */
@Composable
fun ColaEnviosScreen(
    container: AppContainer,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendientes by remember { mutableStateOf<List<ActaPendiente>>(emptyList()) }
    var completadas by remember { mutableStateOf<List<ActaCompletada>>(emptyList()) }

    LaunchedEffect(Unit) {
        container.database.actaPendienteDao().observarTodas().collectLatest { pendientes = it }
    }
    LaunchedEffect(Unit) {
        container.database.actaCompletadaDao().observarTodas().collectLatest { completadas = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(ImpepacPurple900, ImpepacPurple500, ImpepacPurple300, Color.White)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("IMPEPAC Morelos", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                Text("Cola de envíos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pendientes.isEmpty() && completadas.isEmpty()) {
                item {
                    Text(
                        "No hay actas pendientes ni recientes. Todo al día.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }

            if (pendientes.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Pendientes de subir (${pendientes.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        TextButton(onClick = { programarSubidaPendientes(context) }) {
                            Text("Reintentar todas")
                        }
                    }
                }
                items(pendientes, key = { it.id }) { pendiente ->
                    TarjetaPendiente(
                        pendiente = pendiente,
                        onReintentar = { programarSubidaPendientes(context) },
                        onDescartar = {
                            scope.launch {
                                File(pendiente.rutaArchivo).delete()
                                container.database.actaPendienteDao().eliminar(pendiente)
                            }
                        },
                    )
                }
            }

            if (completadas.isNotEmpty()) {
                item {
                    Text(
                        "Enviadas recientemente (${completadas.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
                items(completadas, key = { it.id }) { completada ->
                    TarjetaCompletada(
                        completada = completada,
                        onDescartar = {
                            scope.launch { container.database.actaCompletadaDao().eliminar(completada) }
                        },
                    )
                }
            }
        }
    }
}

private val VerdeExito = Color(0xFF15803D)
private val VerdeExitoFondo = Color(0xFFF0FDF4)

@Composable
internal fun TarjetaCompletada(completada: ActaCompletada, onDescartar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VerdeExitoFondo),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "✓ Acta de la casilla ${completada.casillaId} recibida",
                fontWeight = FontWeight.SemiBold,
                color = VerdeExito,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("Folio: ${completada.folio}", fontSize = 13.sp, color = VerdeExito)
            Text(
                "Enviada: ${formatoHora.format(Date(completada.completadaEn))}",
                fontSize = 12.sp,
                color = VerdeExito.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDescartar) { Text("Descartar") }
        }
    }
}

@Composable
internal fun TarjetaPendiente(
    pendiente: ActaPendiente,
    onReintentar: () -> Unit,
    onDescartar: () -> Unit,
) {
    val esErrorPermanente = pendiente.estado == EstadoCola.ERROR_PERMANENTE
    Card(
        colors = CardDefaults.cardColors(containerColor = ImpepacMagenta50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (esErrorPermanente) "No se pudo subir el acta de la casilla ${pendiente.casillaId}"
                else "Acta de la casilla ${pendiente.casillaId} pendiente de subir",
                fontWeight = FontWeight.SemiBold,
                color = ImpepacMagenta600,
                fontSize = 14.sp,
            )
            Text(
                "En espera desde: ${formatoHora.format(Date(pendiente.creadoEn))}",
                fontSize = 12.sp,
                color = ImpepacMagenta600.copy(alpha = 0.8f),
            )
            if (esErrorPermanente && pendiente.ultimoError != null) {
                Spacer(Modifier.height(4.dp))
                Text(pendiente.ultimoError, fontSize = 13.sp, color = ImpepacMagenta600)
            } else if (pendiente.intentos > 0) {
                Spacer(Modifier.height(4.dp))
                Text("Sin conexión · intento ${pendiente.intentos}", fontSize = 13.sp, color = ImpepacMagenta600)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                if (!esErrorPermanente) {
                    TextButton(onClick = onReintentar) { Text("Reintentar ahora") }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDescartar) { Text("Descartar") }
            }
        }
    }
}
