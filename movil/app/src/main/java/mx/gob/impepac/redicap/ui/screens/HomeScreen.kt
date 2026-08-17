package mx.gob.impepac.redicap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.local.ActaCompletada
import mx.gob.impepac.redicap.data.local.ActaPendiente
import mx.gob.impepac.redicap.data.model.UsuarioResponse
import mx.gob.impepac.redicap.data.network.llamar
import mx.gob.impepac.redicap.ui.theme.ImpepacMagenta50
import mx.gob.impepac.redicap.ui.theme.ImpepacMagenta600
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple300
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple500
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple900

@Composable
fun HomeScreen(
    container: AppContainer,
    onDigitalizar: (casillaAsignadaId: Long?) -> Unit,
    onVerCola: () -> Unit,
    onLogout: () -> Unit,
) {
    var perfil by remember { mutableStateOf<UsuarioResponse?>(null) }
    var casillaAsignadaEtiqueta by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendientes by remember { mutableStateOf<List<ActaPendiente>>(emptyList()) }
    var completadas by remember { mutableStateOf<List<ActaCompletada>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        llamar { container.api.obtenerPerfil() }
            .onSuccess { perfil = it }
            .onFailure { error = it.message }
    }

    LaunchedEffect(perfil?.casillaAsignadaId) {
        val id = perfil?.casillaAsignadaId ?: return@LaunchedEffect
        llamar { container.api.obtenerCasilla(id) }.onSuccess { c ->
            casillaAsignadaEtiqueta = "Sección ${c.numeroSeccion} · ${c.tipo} ${c.numeroCasilla} · " +
                "${c.municipioNombre} · ${c.distritoNombre}"
        }
    }

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

            if (pendientes.isNotEmpty() || completadas.isNotEmpty()) {
                ResumenColaEnvios(
                    pendientes = pendientes.size,
                    completadas = completadas.size,
                    onVerCola = onVerCola,
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
                            Text("Casilla asignada: ${casillaAsignadaEtiqueta ?: "cargando…"}", fontSize = 14.sp)
                        } else {
                            Text(
                                "No tienes una casilla asignada todavía. Puedes elegir cualquier casilla del catálogo al digitalizar.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onDigitalizar(p.casillaAsignadaId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Digitalizar acta")
                }
            }
        }
    }
}

/** Resumen tocable: cuenta rápida de la cola, el detalle completo vive en ColaEnviosScreen. */
@Composable
private fun ResumenColaEnvios(pendientes: Int, completadas: Int, onVerCola: () -> Unit) {
    val colorFondo = if (pendientes > 0) ImpepacMagenta50 else Color(0xFFF0FDF4)
    val colorTexto = if (pendientes > 0) ImpepacMagenta600 else Color(0xFF15803D)
    Card(
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        modifier = Modifier.fillMaxWidth(),
        onClick = onVerCola,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Cola de envíos", fontWeight = FontWeight.SemiBold, color = colorTexto, fontSize = 14.sp)
                val resumen = buildList {
                    if (pendientes > 0) add("$pendientes pendiente${if (pendientes == 1) "" else "s"}")
                    if (completadas > 0) add("$completadas enviada${if (completadas == 1) "" else "s"}")
                }.joinToString(" · ")
                Text(resumen, color = colorTexto, fontSize = 13.sp)
            }
            TextButton(onClick = onVerCola) { Text("Ver todo") }
        }
    }
}
