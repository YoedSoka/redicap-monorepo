package mx.gob.impepac.redicap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.model.CasillaResponse
import mx.gob.impepac.redicap.data.model.DistritoResponse
import mx.gob.impepac.redicap.data.model.MunicipioResponse
import mx.gob.impepac.redicap.data.model.SeccionResponse
import mx.gob.impepac.redicap.data.network.llamar

/** Elecciones simultáneas del proceso (DFR R4/R5/R6); cada una produce un acta independiente. */
private val ELECCIONES = listOf(
    "GUBERNATURA" to "Gubernatura",
    "DIPUTACION_LOCAL" to "Diputación Local",
    "AYUNTAMIENTO" to "Ayuntamiento",
)

/**
 * Selector encadenado Distrito -> Municipio -> Sección -> Casilla, más la elección a digitalizar
 * (una casilla produce hasta 3 actas independientes, una por elección).
 * Si el usuario tiene una casilla preasignada, se usa como punto de partida
 * (sigue siendo el valor por defecto), pero se puede cambiar libremente.
 */
@Composable
fun SeleccionCasillaScreen(
    container: AppContainer,
    casillaAsignadaId: Long?,
    onCasillaElegida: (casillaId: Long, tipoEleccion: String) -> Unit,
    onVolver: () -> Unit,
) {
    var tipoEleccionElegida by remember { mutableStateOf(ELECCIONES.first().first) }
    var distritos by remember { mutableStateOf<List<DistritoResponse>>(emptyList()) }
    var municipios by remember { mutableStateOf<List<MunicipioResponse>>(emptyList()) }
    var secciones by remember { mutableStateOf<List<SeccionResponse>>(emptyList()) }
    var casillas by remember { mutableStateOf<List<CasillaResponse>>(emptyList()) }

    var distritoElegido by remember { mutableStateOf<DistritoResponse?>(null) }
    var municipioElegido by remember { mutableStateOf<MunicipioResponse?>(null) }
    var seccionElegida by remember { mutableStateOf<SeccionResponse?>(null) }
    var casillaElegida by remember { mutableStateOf<CasillaResponse?>(null) }

    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Carga inicial: distritos, municipios, y si hay casilla preasignada, resolvemos su
    // jerarquía completa (municipio → sección → casilla) de forma secuencial para
    // preseleccionarla — sigue siendo el default, pero el usuario puede cambiar cualquier
    // nivel libremente después.
    LaunchedEffect(Unit) {
        val resDistritos = llamar { container.api.listarDistritos() }
        val resMunicipios = llamar { container.api.listarMunicipios() }
        resDistritos.onSuccess { distritos = it }.onFailure { error = it.message }
        resMunicipios.onSuccess { municipios = it }.onFailure { error = it.message }

        if (casillaAsignadaId != null) {
            llamar { container.api.obtenerCasilla(casillaAsignadaId) }.onSuccess { casilla ->
                val municipio = resMunicipios.getOrNull()?.find { it.nombre == casilla.municipioNombre }
                distritoElegido = resDistritos.getOrNull()?.find { it.nombre == casilla.distritoNombre }
                municipioElegido = municipio

                if (municipio != null) {
                    llamar { container.api.listarSecciones(municipio.id) }.onSuccess { listaSecciones ->
                        secciones = listaSecciones
                        val seccion = listaSecciones.find { it.id == casilla.seccionId }
                        seccionElegida = seccion
                        if (seccion != null) {
                            llamar { container.api.listarCasillas(seccion.id) }.onSuccess { listaCasillas ->
                                casillas = listaCasillas
                                casillaElegida = listaCasillas.find { it.id == casillaAsignadaId }
                            }
                        }
                    }
                }
            }
        }
        cargando = false
    }

    // Recargar secciones cuando cambia el municipio elegido (por el usuario o el bootstrap
    // de arriba — repetir el fetch ahí es inofensivo, solo evita perder la preselección).
    LaunchedEffect(municipioElegido) {
        val municipioId = municipioElegido?.id
        if (municipioId == null) {
            secciones = emptyList()
            return@LaunchedEffect
        }
        llamar { container.api.listarSecciones(municipioId) }
            .onSuccess { secciones = it }
            .onFailure { error = it.message }
    }

    // Recargar casillas cuando cambia la sección elegida.
    LaunchedEffect(seccionElegida) {
        val seccionId = seccionElegida?.id
        if (seccionId == null) {
            casillas = emptyList()
            return@LaunchedEffect
        }
        llamar { container.api.listarCasillas(seccionId) }
            .onSuccess { casillas = it }
            .onFailure { error = it.message }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Elige la casilla a digitalizar", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        if (casillaAsignadaId != null) {
            Text(
                "Tu casilla asignada aparece preseleccionada; puedes cambiarla si vas a digitalizar otra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))

        if (cargando) {
            CircularProgressIndicator()
        } else {
            SelectorDesplegable(
                etiqueta = "Elección",
                opciones = ELECCIONES,
                seleccionado = ELECCIONES.find { it.first == tipoEleccionElegida },
                etiquetaDe = { it.second },
                onSeleccionar = { tipoEleccionElegida = it.first },
            )
            Spacer(Modifier.height(12.dp))
            SelectorDesplegable(
                etiqueta = "Distrito",
                opciones = distritos,
                seleccionado = distritoElegido,
                etiquetaDe = { "${it.clave} · ${it.nombre}" },
                onSeleccionar = {
                    distritoElegido = it
                    municipioElegido = null
                    seccionElegida = null
                    casillaElegida = null
                },
            )
            Spacer(Modifier.height(12.dp))
            SelectorDesplegable(
                etiqueta = "Municipio",
                opciones = municipios,
                seleccionado = municipioElegido,
                etiquetaDe = { "${it.clave} · ${it.nombre}" },
                habilitado = distritoElegido != null,
                onSeleccionar = {
                    municipioElegido = it
                    seccionElegida = null
                    casillaElegida = null
                },
            )
            Spacer(Modifier.height(12.dp))
            SelectorDesplegable(
                etiqueta = "Sección",
                opciones = secciones,
                seleccionado = seccionElegida,
                etiquetaDe = { "Sección ${it.numeroSeccion}" },
                habilitado = municipioElegido != null,
                onSeleccionar = {
                    seccionElegida = it
                    casillaElegida = null
                },
            )
            Spacer(Modifier.height(12.dp))
            SelectorDesplegable(
                etiqueta = "Casilla",
                opciones = casillas,
                seleccionado = casillaElegida,
                etiquetaDe = { "${it.tipo} ${it.numeroCasilla}" },
                habilitado = seccionElegida != null,
                onSeleccionar = { casillaElegida = it },
            )
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { casillaElegida?.let { onCasillaElegida(it.id, tipoEleccionElegida) } },
            enabled = casillaElegida != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continuar")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectorDesplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    etiquetaDe: (T) -> String,
    habilitado: Boolean = true,
    onSeleccionar: (T) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = { if (habilitado) expandido = it },
    ) {
        OutlinedTextField(
            value = seleccionado?.let(etiquetaDe) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expandido && habilitado, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(etiquetaDe(opcion)) },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    },
                )
            }
        }
    }
}
