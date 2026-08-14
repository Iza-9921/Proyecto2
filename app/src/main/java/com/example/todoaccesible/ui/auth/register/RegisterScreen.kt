package com.example.todoaccesible.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.BigTouchOutlinedButton
import com.example.todoaccesible.core.designsystem.EmpresaInfoFields

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateBack: () -> Unit,
    onRegistrationBlocked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(if (uiState.step == 1) "Crear cuenta · Paso 1 de 2" else "Registro de la empresa · Paso 2 de 2") })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.step == 1) {
                OutlinedTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Contraseña") },
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
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
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

                if (uiState.error != null) {
                    Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }

                BigTouchButton(
                    text = "Siguiente",
                    onClick = viewModel::goToStep2
                )
                TextButton(onClick = onNavigateBack) {
                    Text("Ya tengo cuenta, iniciar sesión")
                }
            } else {
                Text(
                    "Estos datos aparecen en la portada del scorecard.",
                    style = MaterialTheme.typography.bodyMedium
                )
                EmpresaInfoFields(
                    projectName = uiState.projectName,
                    onProjectNameChange = viewModel::onProjectNameChange,
                    clienteNombre = uiState.clienteNombre,
                    onClienteNombreChange = viewModel::onClienteNombreChange,
                    telefono = uiState.telefono,
                    onTelefonoChange = viewModel::onTelefonoChange,
                    ubicacion = uiState.ubicacion,
                    onUbicacionChange = viewModel::onUbicacionChange,
                    entidadFederativa = uiState.entidadFederativa,
                    onEntidadFederativaChange = viewModel::onEntidadFederativaChange,
                    ciudad = uiState.ciudad,
                    onCiudadChange = viewModel::onCiudadChange,
                    tipoInmueble = uiState.tipoInmueble,
                    onTipoInmuebleChange = viewModel::onTipoInmuebleChange,
                    fechaEvaluacion = uiState.fechaEvaluacion,
                    onFechaEvaluacionChange = viewModel::onFechaEvaluacionChange,
                    responsable = uiState.responsable,
                    onResponsableChange = viewModel::onResponsableChange,
                    revision = uiState.revision,
                    onRevisionChange = viewModel::onRevisionChange,
                    logoEmpresaUri = uiState.logoEmpresaUri,
                    onLogoEmpresaChange = viewModel::onLogoEmpresaChange,
                    showRevision = false
                )

                if (uiState.error != null) {
                    Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BigTouchOutlinedButton(
                        text = "Atrás",
                        enabled = !uiState.loading,
                        onClick = viewModel::backToStep1,
                        modifier = Modifier.weight(1f)
                    )
                    BigTouchButton(
                        text = if (uiState.loading) "Registrando…" else "Registrarse",
                        enabled = !uiState.loading && uiState.projectName.isNotBlank(),
                        onClick = { viewModel.register() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (uiState.showActivationNotice) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cuenta creada con éxito") },
            text = {
                Text(
                    "Tu cuenta se creó correctamente, pero está bloqueada. Debes contactar al administrador para que la habilite. " +
                        "Una vez que la habilite, inicia sesión con tu correo y contraseña para hacer los diagnósticos que te permita."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeActivationNotice(onRegistrationBlocked) }) { Text("Entendido") }
            }
        )
    }
}
