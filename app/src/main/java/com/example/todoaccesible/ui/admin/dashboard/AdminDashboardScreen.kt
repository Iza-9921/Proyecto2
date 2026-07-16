package com.example.todoaccesible.ui.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.CreditBar
import com.example.todoaccesible.core.designsystem.nivelColor
import com.example.todoaccesible.domain.scoring.CreditScore

@Composable
fun AdminDashboardScreen(viewModel: AdminDashboardViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de administración") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(label = "Diagnósticos", value = uiState.total.toString(), modifier = Modifier.weight(1f))
                StatTile(
                    label = "Validados",
                    value = (uiState.porEstado[com.example.todoaccesible.data.model.DiagnosticStatus.VALIDADO] ?: 0).toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Por estado", style = MaterialTheme.typography.titleMedium)
                    if (uiState.total == 0) {
                        Text(
                            "Todavía no hay diagnósticos enviados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        com.example.todoaccesible.data.model.DiagnosticStatus.entries.forEach { status ->
                            val count = uiState.porEstado[status] ?: 0
                            if (count > 0) {
                                CreditBar(
                                    label = status.label,
                                    score = CreditScore(count, uiState.total, count * 100 / uiState.total),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Por nivel alcanzado", style = MaterialTheme.typography.titleMedium)
                    val totalConNivel = uiState.porNivel.values.sum()
                    if (totalConNivel == 0) {
                        Text(
                            "Todavía no hay diagnósticos validados con nivel calculado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        com.example.todoaccesible.data.model.Nivel.entries.forEach { nivel ->
                            val count = uiState.porNivel[nivel] ?: 0
                            CreditBar(
                                label = nivel.label,
                                score = CreditScore(count, totalConNivel, count * 100 / totalConNivel),
                                color = nivelColor(nivel)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
