package com.example.todoaccesible.ui.cliente.diagnostic.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.DiagnosticHistorySection
import com.example.todoaccesible.core.designsystem.DiagnosticStatusChip
import com.example.todoaccesible.core.designsystem.ScorecardHeaderCard
import com.example.todoaccesible.core.designsystem.SectionScoreRow
import com.example.todoaccesible.data.model.DiagnosticStatus

@Composable
fun DiagnosticDetailScreen(
    viewModel: DiagnosticDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.diagnostic?.projectName ?: "Diagnóstico") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportPdf(context) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Exportar PDF")
                    }
                }
            )
        }
    ) { innerPadding ->
        val scorecard = uiState.scorecard
        val diagnostic = uiState.diagnostic

        if (uiState.loading || scorecard == null || diagnostic == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticStatusChip(status = diagnostic.estado)
                    Text(diagnostic.ubicacion, style = MaterialTheme.typography.bodyMedium)
                    Text("Responsable: ${diagnostic.responsable}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                ScorecardHeaderCard(
                    nivel = scorecard.nivel,
                    required = scorecard.required,
                    plus = scorecard.plus
                )
            }
            item {
                Text(
                    if (diagnostic.estado == DiagnosticStatus.VALIDADO) "Resultado oficial" else "Resultado preliminar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                BigTouchButton(
                    text = "Descargar PDF",
                    onClick = { viewModel.exportPdf(context) }
                )
            }
            item {
                Text("Detalle por sección", style = MaterialTheme.typography.titleMedium)
            }
            items(scorecard.sections, key = { it.seccionId }) { section ->
                SectionScoreRow(section = section)
            }
            if (uiState.history.isNotEmpty()) {
                item { DiagnosticHistorySection(entries = uiState.history) }
            }
        }
    }

    uiState.exportError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExportError,
            title = { Text("Cuestionario incompleto") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissExportError) { Text("Entendido") }
            }
        )
    }
}
