package com.example.todoaccesible.ui.cliente.diagnostic.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.todoaccesible.core.designsystem.BigTouchOutlinedButton
import com.example.todoaccesible.core.designsystem.ScorecardHeaderCard

/**
 * Pantalla "Resultado del diagnóstico": único destino al que se navega justo
 * después de presionar "Finalizar diagnóstico" en el cuestionario. No se
 * puede regresar al cuestionario ni al Home desde aquí salvo con el botón
 * "Finalizar" de esta pantalla (RF: flujo post-envío).
 */
@Composable
fun DiagnosticResultScreen(
    viewModel: DiagnosticResultViewModel,
    onFinalizar: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = true) {}

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Resultado del diagnóstico") })
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Diagnóstico enviado correctamente.",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Resultado preliminar. El resultado oficial estará disponible cuando el administrador finalice la revisión.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ScorecardHeaderCard(
                nivel = scorecard.nivel,
                required = scorecard.required,
                plus = scorecard.plus
            )

            Text(
                text = "Nivel obtenido: ${scorecard.nivel.label}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Porcentaje Required: ${scorecard.required.pct}%  ·  Plus: ${scorecard.plus.pct}%",
                style = MaterialTheme.typography.bodyLarge
            )

            BigTouchButton(
                text = if (uiState.exporting) "Generando…" else "Descargar PDF",
                enabled = !uiState.exporting,
                onClick = { viewModel.exportPdf(context) }
            )
            BigTouchOutlinedButton(
                text = if (uiState.exporting) "Generando…" else "Descargar Excel",
                enabled = !uiState.exporting,
                onClick = { viewModel.exportExcel(context) }
            )
            BigTouchButton(
                text = "Finalizar",
                onClick = {
                    android.util.Log.d("DiagnosticResultScreen", "Finalizar tocado")
                    onFinalizar()
                }
            )
        }
    }

    uiState.exportError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExportError,
            title = { Text("No se pudo generar el archivo") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissExportError) { Text("Entendido") }
            }
        )
    }
}
