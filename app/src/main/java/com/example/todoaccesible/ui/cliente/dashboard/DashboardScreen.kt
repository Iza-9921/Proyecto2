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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            FloatingActionButton(onClick = onNavigateToNewDiagnostic) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo diagnóstico")
            }
        }
    ) { innerPadding ->
        if (diagnostics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aún no tienes diagnósticos. Toca + para crear el primero.",
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
