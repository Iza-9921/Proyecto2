package com.example.todoaccesible.ui.admin.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.theme.EstadoAprobado
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role

@Composable
fun UserManagementScreen(viewModel: UserManagementViewModel) {
    val users by viewModel.users.collectAsState()
    val activeSessionUserIds by viewModel.activeSessionUserIds.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Usuarios") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreateDialog) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo usuario")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                UserRow(
                    user = user,
                    sessionActive = user.id in activeSessionUserIds,
                    onRoleChange = { viewModel.updateRole(user.id, it) },
                    onLicenseToggle = { viewModel.setLicenseActive(user.id, it) },
                    onForceCloseSession = { viewModel.forceCloseSession(user.id) },
                    onDelete = { viewModel.deleteUser(user.id) },
                    onDiagnosticosDisponiblesChange = { viewModel.setDiagnosticosDisponibles(user.id, it) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(viewModel)
    }
}

@Composable
private fun UserRow(
    user: UserEntity,
    sessionActive: Boolean,
    onRoleChange: (Role) -> Unit,
    onLicenseToggle: (Boolean) -> Unit,
    onForceCloseSession: () -> Unit,
    onDelete: () -> Unit,
    onDiagnosticosDisponiblesChange: (Int?) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(user.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    Box {
                        TextButton(onClick = { menuExpanded = true }) {
                            Text(if (user.rol == Role.ADMIN) "Admin" else "Cliente")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Cliente") }, onClick = { onRoleChange(Role.CLIENTE); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Admin") }, onClick = { onRoleChange(Role.ADMIN); menuExpanded = false })
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar usuario")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text("Licencia activa", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = user.licenseActive,
                        onCheckedChange = onLicenseToggle,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (sessionActive) {
                    Chip(
                        label = "Sesión activa (toca para forzar cierre)",
                        color = EstadoAprobado,
                        modifier = Modifier.clickable(onClick = onForceCloseSession)
                    )
                }
            }
            if (user.rol == Role.CLIENTE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Diagnósticos disponibles", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        val cantidadActual = user.diagnosticosDisponibles ?: 0
                        TextButton(
                            onClick = { onDiagnosticosDisponiblesChange((cantidadActual - 1).coerceAtLeast(0)) },
                            enabled = cantidadActual > 0
                        ) { Text("−") }
                        Text(
                            cantidadActual.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        TextButton(
                            onClick = { onDiagnosticosDisponiblesChange(cantidadActual + 1) }
                        ) { Text("+") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateUserDialog(viewModel: UserManagementViewModel) {
    val form by viewModel.form.collectAsState()
    val error by viewModel.error.collectAsState()

    AlertDialog(
        onDismissRequest = viewModel::dismissCreateDialog,
        title = { Text("Nuevo usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = form.nombre, onValueChange = viewModel::onNombreChange, label = { Text("Nombre") })
                OutlinedTextField(value = form.email, onValueChange = viewModel::onEmailChange, label = { Text("Correo") })
                OutlinedTextField(value = form.password, onValueChange = viewModel::onPasswordChange, label = { Text("Contraseña") })
                Row {
                    TextButton(onClick = { viewModel.onRoleChange(Role.CLIENTE) }) {
                        Text(if (form.rol == Role.CLIENTE) "● Cliente" else "○ Cliente")
                    }
                    TextButton(onClick = { viewModel.onRoleChange(Role.ADMIN) }) {
                        Text(if (form.rol == Role.ADMIN) "● Admin" else "○ Admin")
                    }
                }
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            BigTouchButton(text = "Crear", onClick = viewModel::createUser)
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissCreateDialog) { Text("Cancelar") }
        }
    )
}
