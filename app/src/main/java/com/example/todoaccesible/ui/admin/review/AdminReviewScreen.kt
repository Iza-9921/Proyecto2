package com.example.todoaccesible.ui.admin.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.example.todoaccesible.core.designsystem.AnswerValueChip
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.DiagnosticStatus

@Composable
fun AdminReviewScreen(
    viewModel: AdminReviewViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedPhotoUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.diagnostic?.projectName ?: "Revisión") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportPdf(context) }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF")
                    }
                    IconButton(onClick = { viewModel.exportExcel(context) }) {
                        Icon(Icons.Filled.TableChart, contentDescription = "Exportar Excel")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { viewModel.setStatus(DiagnosticStatus.RECHAZADO) }, modifier = Modifier.weight(1f)) {
                    Text("Rechazar")
                }
                TextButton(onClick = { viewModel.setStatus(DiagnosticStatus.INFO_REQUERIDA) }, modifier = Modifier.weight(1f)) {
                    Text("Solicitar info")
                }
                TextButton(onClick = { viewModel.setStatus(DiagnosticStatus.VALIDADO) }, modifier = Modifier.weight(1f)) {
                    Text("Aprobar")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::setQuery,
                label = { androidx.compose.material3.Text("Buscar pregunta") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = uiState.creditoFilter == null, onClick = { viewModel.setCreditoFilter(null) }, label = { Text("Todos") })
                FilterChip(selected = uiState.creditoFilter == Credito.REQUIRED, onClick = { viewModel.setCreditoFilter(Credito.REQUIRED) }, label = { Text("Required") })
                FilterChip(selected = uiState.creditoFilter == Credito.PLUS, onClick = { viewModel.setCreditoFilter(Credito.PLUS) }, label = { Text("Plus") })
                AnswerValue.entries.forEach { value ->
                    FilterChip(
                        selected = uiState.valorFilter == value,
                        onClick = { viewModel.setValorFilter(if (uiState.valorFilter == value) null else value) },
                        label = { Text(value.short) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredRows, key = { it.question.codigo }) { row ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(row.question.codigo, style = MaterialTheme.typography.labelLarge)
                                Chip(
                                    label = if (row.question.credito == Credito.REQUIRED) "Required" else "Plus",
                                    color = if (row.question.credito == Credito.REQUIRED) RequiredNavy else PlusFuchsia
                                )
                                AnswerValueChip(value = row.answer?.valor)
                            }
                            Text(row.question.concepto, style = MaterialTheme.typography.bodyLarge)
                            if (!row.answer?.comentario.isNullOrBlank()) {
                                Text(
                                    "Observación: ${row.answer?.comentario}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (row.photos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(row.photos, key = { it.id }) { photo ->
                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = "Evidencia de ${row.question.codigo}, toca para ampliar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { expandedPhotoUrl = photo.uri.toString() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    expandedPhotoUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { expandedPhotoUrl = null },
            confirmButton = {
                TextButton(onClick = { expandedPhotoUrl = null }) { Text("Cerrar") }
            },
            text = {
                AsyncImage(
                    model = url,
                    contentDescription = "Foto de evidencia ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
