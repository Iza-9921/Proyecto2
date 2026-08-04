package com.example.todoaccesible.ui.admin.pending

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.DiagnosticStatusChip
import com.example.todoaccesible.core.designsystem.NivelChip
import com.example.todoaccesible.data.model.DiagnosticStatus

@Composable
fun AdminPendingScreen(
    viewModel: AdminPendingViewModel,
    onOpenReview: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Diagnósticos") }) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TableView(
                uiState = uiState,
                onQueryChange = viewModel::setQuery,
                onFilterChange = viewModel::setFilterEstado,
                onOpenReview = onOpenReview
            )
        }
    }
}

@Composable
private fun TableView(
    uiState: AdminPendingUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (DiagnosticStatus?) -> Unit,
    onOpenReview: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = { Text("Buscar por proyecto o cliente") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = uiState.filterEstado == null, onClick = { onFilterChange(null) }, label = { Text("Todos") })
            DiagnosticStatus.entries.filter { it != DiagnosticStatus.BORRADOR }.forEach { estado ->
                FilterChip(
                    selected = uiState.filterEstado == estado,
                    onClick = { onFilterChange(estado) },
                    label = { Text(estado.label) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filtered, key = { it.id }) { diagnostic ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpenReview(diagnostic.id) }) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(diagnostic.projectName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            uiState.usersById[diagnostic.clienteId]?.nombre ?: "Cliente",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiagnosticStatusChip(status = diagnostic.estado)
                            diagnostic.nivel?.let { NivelChip(it) }
                        }
                    }
                }
            }
        }
    }
}
