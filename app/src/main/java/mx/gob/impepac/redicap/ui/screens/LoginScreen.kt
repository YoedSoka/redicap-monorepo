package mx.gob.impepac.redicap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.gob.impepac.redicap.data.AppContainer
import mx.gob.impepac.redicap.data.network.llamar
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple300
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple500
import mx.gob.impepac.redicap.ui.theme.ImpepacPurple900

@Composable
fun LoginScreen(container: AppContainer, onLoginExitoso: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(ImpepacPurple900, ImpepacPurple500, ImpepacPurple300, Color.White)
                    )
                )
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("IMPEPAC Morelos", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Text(
                "REDICAP Digitalizador",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    error = null
                    cargando = true
                    scope.launch {
                        val resultado = llamar {
                            val token = container.api.login(
                                mx.gob.impepac.redicap.data.model.LoginRequest(username, password)
                            )
                            if (token.rol != "DIGITALIZADOR") {
                                throw RuntimeException("Esta app es solo para digitalizadores")
                            }
                            container.tokenStore.guardarSesion(token.token, token.username, token.rol)
                        }
                        cargando = false
                        resultado.onSuccess { onLoginExitoso() }
                            .onFailure { error = it.message }
                    }
                },
                enabled = !cargando && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (cargando) "Ingresando…" else "Ingresar")
            }
        }
    }
}
