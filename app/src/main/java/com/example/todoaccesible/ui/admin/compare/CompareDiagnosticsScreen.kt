package com.example.todoaccesible.ui.admin.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.ScorecardHeaderCard
import com.example.todoaccesible.core.designsystem.SectionScoreRow
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.domain.scoring.ScorecardResult

@Composable
fun CompareDiagnosticsScreen(viewModel: CompareDiagnosticsViewModel) {
    val diagnostics by viewModel.allDiagnostics.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Comparar diagnósticos") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DiagnosticPicker(
                label = "Diagnóstico A",
                diagnostics = diagnostics,
                selected = uiState.diagnosticA,
                onSelect = viewModel::selectA
            )
            uiState.scorecardA?.let { CompareSummary(it) }

            DiagnosticPicker(
                label = "Diagnóstico B",
                diagnostics = diagnostics,
                selected = uiState.diagnosticB,
                onSelect = viewModel::selectB
            )
            uiState.scorecardB?.let { CompareSummary(it) }
        }
    }
}

@Composable
private fun CompareSummary(scorecard: ScorecardResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScorecardHeaderCard(nivel = scorecard.nivel, required = scorecard.required, plus = scorecard.plus)
        scorecard.sections.forEach { section -> SectionScoreRow(section = section) }
    }
}

@Composable
private fun DiagnosticPicker(
    label: String,
    diagnostics: List<DiagnosticEntity>,
    selected: DiagnosticEntity?,
    onSelect: (DiagnosticEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.projectName ?: "Selecciona un diagnóstico")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                diagnostics.forEach { diagnostic ->
                    DropdownMenuItem(
                        text = { Text(diagnostic.projectName) },
                        onClick = { onSelect(diagnostic); expanded = false }
                    )
                }
            }
        }
    }
}
