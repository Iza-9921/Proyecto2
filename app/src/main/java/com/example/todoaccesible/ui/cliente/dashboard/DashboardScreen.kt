package com.example.todoaccesible.ui.cliente.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.DiagnosticStatusChip
import com.example.todoaccesible.core.designsystem.NivelChip
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNewDiagnostic: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val diagnostics by viewModel.diagnostics.collectAsState()
    val unreadNotifications by viewModel.unreadNotifications.collectAsState()
    val diagnosticosDisponibles by viewModel.diagnosticosDisponibles.collectAsState()
    var showQuotaBlockedDialog by remember { mutableStateOf(false) }

    fun requestNewDiagnostic() {
        val hasDraft = diagnostics.any { it.estado == DiagnosticStatus.BORRADOR }
        if (hasDraft || diagnosticosDisponibles == null || (diagnosticosDisponibles ?: 0) > 0) {
            onNavigateToNewDiagnostic()
        } else {
            showQuotaBlockedDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis diagnósticos") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(badge = {
                            if (unreadNotifications > 0) Badge { Text(unreadNotifications.toString()) }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones ($unreadNotifications sin leer)")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::requestNewDiagnostic) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo diagnóstico")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                text = "Diagnósticos disponibles: ${diagnosticosDisponibles?.toString() ?: "Ilimitados"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (diagnostics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aún no tienes diagnósticos. Toca + para crear el primero.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(diagnostics, key = { it.id }) { diagnostic ->
                        DiagnosticCard(
                            diagnostic = diagnostic,
                            onClick = {
                                if (diagnostic.estado == DiagnosticStatus.BORRADOR) {
                                    onNavigateToNewDiagnostic()
                                } else {
                                    onNavigateToDetail(diagnostic.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showQuotaBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showQuotaBlockedDialog = false },
            title = { Text("Sin diagnósticos disponibles") },
            text = { Text("No cuentas con diagnósticos disponibles. Comunícate con la empresa para solicitar la asignación de nuevos diagnósticos.") },
            confirmButton = {
                TextButton(onClick = { showQuotaBlockedDialog = false }) { Text("Aceptar") }
            }
        )
    }
}

@Composable
private fun DiagnosticCard(diagnostic: DiagnosticEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = diagnostic.projectName.ifBlank { "Diagnóstico sin nombre (borrador)" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(diagnostic.ubicacion, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiagnosticStatusChip(status = diagnostic.estado)
                diagnostic.nivel?.let { NivelChip(nivel = it) }
            }
            val fecha = diagnostic.fechaEnvio ?: diagnostic.fechaCreacion
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX")).format(Date(fecha)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
