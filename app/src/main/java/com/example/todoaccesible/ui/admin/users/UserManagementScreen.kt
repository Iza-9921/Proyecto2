package com.example.todoaccesible.ui.admin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.designsystem.ConfirmDialog
import com.example.todoaccesible.core.designsystem.DropdownField
import com.example.todoaccesible.core.theme.EstadoAprobado
import com.example.todoaccesible.core.theme.EstadoNoCumple
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role

@Composable
fun UserManagementScreen(viewModel: UserManagementViewModel) {
    val gruposPorMes by viewModel.gruposPorMes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val collapsedMonths by viewModel.collapsedMonths.collectAsState()
    val mesSeleccionado by viewModel.mesSeleccionado.collectAsState()
    val anioSeleccionado by viewModel.anioSeleccionado.collectAsState()
    val mostrarSelectorMeses by viewModel.mostrarSelectorMeses.collectAsState()
    val empresaTarget by viewModel.empresaTarget.collectAsState()
    val empresaTargetDiagnostico by viewModel.empresaTargetDiagnostico.collectAsState()
    val toggleTarget by viewModel.toggleTarget.collectAsState()
    val activarTarget by viewModel.activarTarget.collectAsState()
    // Se colecta aquí (no solo dentro de ActivarDialog) para que la carga de tipos
    // arranque en cuanto se abre la pantalla, no hasta que el admin ya haya
    // tocado "Activar" — si no, el diálogo se abre con la lista todavía vacía.
    val tiposPrecargados by viewModel.tipos.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Usuarios") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    label = { Text("Buscar por nombre o correo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::abrirSelectorMeses) { Text("Meses ▾") }
                    if (mesSeleccionado != null) {
                        Chip(
                            label = "${MESES_ES[mesSeleccionado!!]} $anioSeleccionado ✕",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = viewModel::limpiarFiltroMes)
                        )
                    }
                }
            }
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gruposPorMes.isEmpty()) {
                    item { Text("No hay usuarios que coincidan.", style = MaterialTheme.typography.bodyMedium) }
                }
                gruposPorMes.forEach { grupo ->
                    val colapsado = grupo.clave in collapsedMonths
                    item(key = "header_${grupo.clave}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleMonthCollapse(grupo.clave) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${if (colapsado) "▸" else "▾"} ${grupo.etiqueta} · ${grupo.clientes.size} usuarios",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (!colapsado) {
                        items(grupo.clientes, key = { it.id }) { user ->
                            UserRow(
                                user = user,
                                onActivar = { viewModel.openActivarDialog(user) },
                                onDesactivar = { viewModel.requestDeactivate(user) },
                                onDelete = { viewModel.deleteUser(user.id) },
                                onDiagnosticosDisponiblesChange = { viewModel.setDiagnosticosDisponibles(user.id, it) },
                                onVerDetalles = { viewModel.openEmpresaDetail(user) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarSelectorMeses) {
        MesesSelectorDialog(
            anio = anioSeleccionado,
            mesSeleccionado = mesSeleccionado,
            onAnioAnterior = { viewModel.cambiarAnioSeleccionado(-1) },
            onAnioSiguiente = { viewModel.cambiarAnioSeleccionado(1) },
            onSeleccionarMes = viewModel::seleccionarMes,
            onLimpiar = viewModel::limpiarFiltroMes,
            onDismiss = viewModel::cerrarSelectorMeses
        )
    }

    toggleTarget?.let { user ->
        ConfirmDialog(
            title = "Desactivar cuenta",
            message = "${user.nombre} no podrá iniciar sesión hasta que reactives su cuenta.",
            confirmLabel = "Desactivar",
            isDestructive = true,
            onConfirm = viewModel::confirmDeactivate,
            onDismiss = viewModel::dismissDeactivate
        )
    }

    activarTarget?.let { user ->
        ActivarDialog(user = user, viewModel = viewModel)
    }

    empresaTarget?.let { user ->
        EmpresaDetailDialog(user = user, diagnostico = empresaTargetDiagnostico, onDismiss = viewModel::closeEmpresaDetail)
    }
}

private val MESES_ES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

@Composable
private fun UserRow(
    user: UserEntity,
    onActivar: () -> Unit,
    onDesactivar: () -> Unit,
    onDelete: () -> Unit,
    onDiagnosticosDisponiblesChange: (Int?) -> Unit,
    onVerDetalles: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(user.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // El rol lo fija el backend en el registro (ADMIN_EMAILS); no es editable desde aquí.
                    Chip(label = if (user.rol == Role.ADMIN) "Admin" else "Cliente", color = MaterialTheme.colorScheme.secondary)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar usuario")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        label = if (user.licenseActive) "Activo" else "Inactivo",
                        color = if (user.licenseActive) EstadoAprobado else EstadoNoCumple
                    )
                    TextButton(onClick = if (user.licenseActive) onDesactivar else onActivar) {
                        Text(if (user.licenseActive) "Desactivar" else "Activar")
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onVerDetalles) { Text("Ver detalles") }
            }
            if (user.rol == Role.CLIENTE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Diagnósticos disponibles", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val cantidadActual = user.diagnosticosDisponibles ?: 0
                        TextButton(
                            onClick = { onDiagnosticosDisponiblesChange((cantidadActual - 1).coerceAtLeast(0)) },
                            enabled = cantidadActual > 0
                        ) { Text("−") }
                        Text(cantidadActual.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
                        TextButton(onClick = { onDiagnosticosDisponiblesChange(cantidadActual + 1) }) { Text("+") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MesesSelectorDialog(
    anio: Int,
    mesSeleccionado: Int?,
    onAnioAnterior: () -> Unit,
    onAnioSiguiente: () -> Unit,
    onSeleccionarMes: (Int) -> Unit,
    onLimpiar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAnioAnterior) { Text("‹") }
                Text(anio.toString(), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAnioSiguiente) { Text("›") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MESES_ES.chunked(3).forEach { fila ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        fila.forEach { nombre ->
                            val index = MESES_ES.indexOf(nombre)
                            val seleccionado = mesSeleccionado == index
                            Text(
                                text = nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSeleccionarMes(index) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onLimpiar) { Text("Ver todos los meses") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun ActivarDialog(user: UserEntity, viewModel: UserManagementViewModel) {
    val tipos by viewModel.tipos.collectAsState()
    val tipoParaAsignar by viewModel.tipoParaAsignar.collectAsState()

    AlertDialog(
        onDismissRequest = viewModel::dismissActivarDialog,
        title = { Text("Activar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${user.nombre} podrá volver a iniciar sesión. Elige qué cuestionario le corresponde responder.")
                DropdownField(
                    label = "Cuestionario a asignar",
                    options = tipos,
                    selected = tipoParaAsignar,
                    onSelected = viewModel::onTipoParaAsignarChange
                )
            }
        },
        confirmButton = { com.example.todoaccesible.core.designsystem.BigTouchButton(text = "Activar", onClick = viewModel::confirmActivar) },
        dismissButton = { TextButton(onClick = viewModel::dismissActivarDialog) { Text("Cancelar") } }
    )
}

@Composable
private fun EmpresaDetailDialog(user: UserEntity, diagnostico: DiagnosticEntity?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalles del cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cuenta", style = MaterialTheme.typography.labelLarge)
                DetailRow("Nombre", user.nombre)
                DetailRow("Correo", user.email)
                DetailRow("Estado", if (user.licenseActive) "Activo" else "Inactivo")
                DetailRow("Evaluaciones", "${user.diagnosticosDisponibles ?: 0}")
                DetailRow("Cuestionario asignado", user.cuestionarioAsignado ?: "—")
                Text("Empresa", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                if (diagnostico != null) {
                    DetailRow("Nombre de la empresa", diagnostico.projectName.ifBlank { "—" })
                    DetailRow("Cliente", diagnostico.clienteNombre.ifBlank { "—" })
                    DetailRow("Dirección", diagnostico.ubicacion.ifBlank { "—" })
                    DetailRow("Ciudad", diagnostico.ciudad.ifBlank { "—" })
                    DetailRow("Estado", diagnostico.entidadFederativa.ifBlank { "—" })
                    DetailRow("Tipo de inmueble", diagnostico.tipoInmueble.ifBlank { "—" })
                    DetailRow("Responsable", diagnostico.responsable.ifBlank { "—" })
                    DetailRow("Teléfono", diagnostico.telefono.ifBlank { "—" })
                } else {
                    Text("Este cliente no registró información de empresa.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
