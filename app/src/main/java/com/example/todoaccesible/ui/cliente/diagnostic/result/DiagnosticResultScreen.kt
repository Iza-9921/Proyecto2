package com.example.todoaccesible.ui.cliente.diagnostic.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.ConfirmDialog
import com.example.todoaccesible.core.designsystem.CreditBar
import com.example.todoaccesible.core.designsystem.NivelChip
import com.example.todoaccesible.core.designsystem.SectionScoreGridCell
import com.example.todoaccesible.core.theme.NivelMagenta
import com.example.todoaccesible.core.theme.NivelOro
import com.example.todoaccesible.core.theme.NivelPlata
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla "Resumen del diagnóstico": se muestra al terminar de contestar
 * las 187 preguntas del cuestionario (RF: flujo post-cuestionario). Es un
 * resultado automático y preliminar; el diagnóstico solo se envía al
 * administrador cuando el usuario toca "Enviar diagnóstico". Desde aquí
 * también se puede volver a revisar respuestas ("Anterior") o descartar
 * el borrador por completo.
 */
@Composable
fun DiagnosticResultScreen(
    viewModel: DiagnosticResultViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onDiscarded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = true) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Resumen del diagnóstico", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Resultado automático y preliminar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Anterior")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.exportPdf(context) },
                        enabled = !uiState.exporting,
                        colors = ButtonDefaults.buttonColors(containerColor = RequiredNavy),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(if (uiState.exporting) "Generando…" else "Exportar PDF")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Anterior")
                        }
                        TextButton(onClick = viewModel::requestDiscard) {
                            Text("Descartar borrador", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = { viewModel.submitDiagnostic(onSubmitted) },
                        enabled = !uiState.submitting,
                        colors = ButtonDefaults.buttonColors(containerColor = PlusFuchsia),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(if (uiState.submitting) "Enviando…" else "Enviar diagnóstico")
                    }
                }
            }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Este resultado es automático y preliminar. Un administrador lo revisará y validará antes del resultado final.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { ProjectSummaryCard(diagnostic) }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Cuantificación parcial del proyecto", style = MaterialTheme.typography.titleMedium)
                        CreditBar(label = "Required", score = scorecard.required, color = RequiredNavy)
                        CreditBar(label = "Plus", score = scorecard.plus, color = PlusFuchsia)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nivel alcanzado:", style = MaterialTheme.typography.titleSmall)
                            NivelChip(scorecard.nivel)
                        }
                    }
                }
            }

            item {
                Text("Detalle por categoría", style = MaterialTheme.typography.titleMedium)
            }

            items(scorecard.sections.withIndex().toList().chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { (index, section) ->
                        SectionScoreGridCell(numero = index + 1, section = section, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { NivelesLegendCard() }
        }
    }

    if (uiState.showDiscardConfirm) {
        ConfirmDialog(
            title = "Descartar borrador",
            message = "Se perderán todas las respuestas capturadas en este diagnóstico. Esta acción no se puede deshacer.",
            confirmLabel = "Descartar",
            isDestructive = true,
            onConfirm = { viewModel.confirmDiscard(onDiscarded) },
            onDismiss = viewModel::dismissDiscard
        )
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

@Composable
private fun ProjectSummaryCard(diagnostic: DiagnosticEntity, modifier: Modifier = Modifier) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
        .format(Date(diagnostic.fechaEvaluacion ?: diagnostic.fechaCreacion))

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen del diagnóstico", style = MaterialTheme.typography.titleMedium)
            SummaryInfoRow("Proyecto", diagnostic.projectName.ifBlank { "—" })
            SummaryInfoRow("Ubicación", diagnostic.ubicacion.ifBlank { "—" })
            SummaryInfoRow("Revisión", diagnostic.revision.ifBlank { "—" })
            SummaryInfoRow("Fecha", dateStr)
            SummaryInfoRow("Responsable", diagnostic.responsable.ifBlank { "—" })
        }
    }
}

@Composable
private fun SummaryInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NivelesLegendCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Niveles distintivos", style = MaterialTheme.typography.titleMedium)
            LegendRow("Plata", "100% créditos Required", NivelPlata)
            HorizontalDivider()
            LegendRow("Oro", "63% al 79% créditos Plus", NivelOro)
            HorizontalDivider()
            LegendRow("Magenta", "80% al 100% créditos Plus", NivelMagenta)
            Text(
                "*Para Oro y Magenta se debe haber cumplido con el 100% de créditos Required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendRow(nombre: String, descripcion: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(28.dp)) {}
        Column {
            Text(nombre, style = MaterialTheme.typography.labelLarge)
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
