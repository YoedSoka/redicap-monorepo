package mx.gob.impepac.redicap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.data.local.EstadoCola
import mx.gob.impepac.redicap.data.model.UsuarioResponse
import mx.gob.impepac.redicap.data.network.llamar
import mx.gob.impepac.redicap.ui.theme.ImpepacMagenta50
import mx.gob.impepac.redicap.ui.theme.ImpepacMagenta600
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple300
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple500
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple900
import mx.gob.impepac.redicap.worker.programarSubidaPendientes
import java.io.File

@Composable
fun HomeScreen(
    container: AppContainer,
    onDigitalizar: (casillaId: Long) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var perfil by remember { mutableStateOf<UsuarioResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendientes by remember { mutableStateOf<List<ActaPendiente>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        llamar { container.api.obtenerPerfil() }
            .onSuccess { perfil = it }
            .onFailure { error = it.message }
    }

    LaunchedEffect(Unit) {
        container.database.actaPendienteDao().observarTodas().collectLatest { pendientes = it }
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
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("IMPEPAC Morelos", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                Text(
                    "REDICAP Digitalizador",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = {
                scope.launch {
                    llamar { container.api.logout() }
                    container.tokenStore.limpiarSesion()
                    onLogout()
                }
            }) {
                Text("Salir", color = Color.White)
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }

            pendientes.forEach { pendiente ->
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
                Spacer(Modifier.height(16.dp))
            }

            perfil?.let { p ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(p.nombreCompleto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(p.username, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        if (p.casillaAsignadaId != null) {
                            Text("Casilla asignada: ${p.casillaAsignadaId}", fontSize = 14.sp)
                        } else {
                            Text(
                                "No tienes una casilla asignada. Contacta al administrador.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                val hayPendienteDeEstaCasilla = pendientes.any { it.casillaId == p.casillaAsignadaId }
                Button(
                    onClick = { p.casillaAsignadaId?.let(onDigitalizar) },
                    enabled = p.casillaAsignadaId != null && !hayPendienteDeEstaCasilla,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Digitalizar acta")
                }
            }
        }
    }
}

@Composable
private fun TarjetaPendiente(
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
