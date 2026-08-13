package com.example.todoaccesible.ui.auth.forgot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Recuperar contraseña") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState.step) {
                ForgotPasswordStep.EMAIL -> {
                    Text(
                        "Ingresa el correo con el que te registraste. Te enviaremos un código de 6 dígitos para restablecer tu contraseña.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ErrorText(uiState.error)
                    BigTouchButton(
                        text = if (uiState.loading) "Enviando…" else "Enviar código",
                        enabled = !uiState.loading,
                        onClick = viewModel::solicitarCodigo
                    )
                }

                ForgotPasswordStep.CODE -> {
                    uiState.infoMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedTextField(
                        value = uiState.codigo,
                        onValueChange = viewModel::onCodigoChange,
                        label = { Text("Código de 6 dígitos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ErrorText(uiState.error)
                    BigTouchButton(
                        text = if (uiState.loading) "Verificando…" else "Verificar código",
                        enabled = !uiState.loading,
                        onClick = viewModel::verificarCodigo
                    )
                    TextButton(onClick = viewModel::reenviarCodigo, enabled = !uiState.loading) {
                        Text("Reenviar código")
                    }
                    TextButton(onClick = viewModel::volverAPedirCodigo, enabled = !uiState.loading) {
                        Text("Usar otro correo")
                    }
                }

                ForgotPasswordStep.NEW_PASSWORD -> {
                    Text("Crea tu nueva contraseña.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = uiState.nuevaContrasena,
                        onValueChange = viewModel::onNuevaContrasenaChange,
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Usa al menos 8 caracteres, con mayúscula, minúscula, número y un signo (ej. @, #, %).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.confirmarContrasena,
                        onValueChange = viewModel::onConfirmarContrasenaChange,
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ErrorText(uiState.error)
                    BigTouchButton(
                        text = if (uiState.loading) "Guardando…" else "Guardar nueva contraseña",
                        enabled = !uiState.loading,
                        onClick = viewModel::establecerNuevaContrasena
                    )
                }

                ForgotPasswordStep.DONE -> {
                    Text(
                        "Tu contraseña se actualizó correctamente. Ya puedes iniciar sesión con ella.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BigTouchButton(text = "Ir a iniciar sesión", onClick = onDone)
                }
            }

            if (uiState.loading) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
