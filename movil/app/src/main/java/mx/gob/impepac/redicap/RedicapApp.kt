package mx.gob.impepac.redicap

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.ui.screens.CapturaScreen
import mx.gob.impepac.redicap.ui.screens.ColaEnviosScreen
import mx.gob.impepac.redicap.ui.screens.HomeScreen
import mx.gob.impepac.redicap.ui.screens.LoginScreen
import mx.gob.impepac.redicap.ui.screens.SeleccionCasillaScreen

private const val RUTA_LOGIN = "login"
private const val RUTA_HOME = "home"
private const val RUTA_COLA = "cola"
private const val RUTA_SELECCION = "seleccion/{casillaAsignadaId}"
private const val RUTA_CAPTURA = "captura/{casillaId}"
/** -1L marca "sin casilla asignada" en la ruta (NavType.LongType no admite null). */
private const val SIN_ASIGNAR = -1L

@Composable
fun RedicapApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var sesionResuelta by remember { mutableStateOf(false) }
    var inicioAutenticado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        inicioAutenticado = container.tokenStore.haySesion()
        sesionResuelta = true
    }

    if (!sesionResuelta) return

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (inicioAutenticado) RUTA_HOME else RUTA_LOGIN,
            modifier = Modifier.padding(padding),
        ) {
            composable(RUTA_LOGIN) {
                LoginScreen(container = container, onLoginExitoso = {
                    navController.navigate(RUTA_HOME) {
                        popUpTo(RUTA_LOGIN) { inclusive = true }
                    }
                })
            }
            composable(RUTA_HOME) {
                HomeScreen(
                    container = container,
                    onDigitalizar = { casillaAsignadaId ->
                        navController.navigate("seleccion/${casillaAsignadaId ?: SIN_ASIGNAR}")
                    },
                    onVerCola = { navController.navigate(RUTA_COLA) },
                    onLogout = {
                        navController.navigate(RUTA_LOGIN) {
                            popUpTo(RUTA_HOME) { inclusive = true }
                        }
                    },
                )
            }
            composable(RUTA_COLA) {
                ColaEnviosScreen(
                    container = container,
                    onVolver = { navController.popBackStack() },
                )
            }
            composable(
                RUTA_SELECCION,
                arguments = listOf(navArgument("casillaAsignadaId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val casillaAsignadaId = backStackEntry.arguments?.getLong("casillaAsignadaId")?.takeIf { it != SIN_ASIGNAR }
                SeleccionCasillaScreen(
                    container = container,
                    casillaAsignadaId = casillaAsignadaId,
                    onCasillaElegida = { casillaId -> navController.navigate("captura/$casillaId") },
                    onVolver = { navController.popBackStack() },
                )
            }
            composable(
                RUTA_CAPTURA,
                arguments = listOf(navArgument("casillaId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val casillaId = backStackEntry.arguments?.getLong("casillaId") ?: 0L
                CapturaScreen(
                    container = container,
                    casillaId = casillaId,
                    onListo = { mensaje ->
                        scope.launch { snackbarHostState.showSnackbar(mensaje) }
                        navController.popBackStack(RUTA_HOME, inclusive = false)
                    },
                    onVolver = { navController.popBackStack() },
                )
            }
        }
    }
}
