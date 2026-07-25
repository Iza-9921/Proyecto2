package com.example.todoaccesible.ui.admin.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.example.todoaccesible.core.designsystem.AnswerValueChip
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.designsystem.DiagnosticHistorySection
import com.example.todoaccesible.core.designsystem.PhotoItem
import com.example.todoaccesible.core.theme.EstadoAprobado
import com.example.todoaccesible.core.theme.EstadoInfoRequerida
import com.example.todoaccesible.core.theme.EstadoNoAplica
import com.example.todoaccesible.core.theme.EstadoNoCumple
import com.example.todoaccesible.core.theme.EstadoPendiente
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.QuestionReviewStatus

private data class PhotoViewerState(val photos: List<PhotoItem>, val initialIndex: Int)

@Composable
fun AdminReviewScreen(
    viewModel: AdminReviewViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportError by viewModel.exportError.collectAsState()
    var expandedPhotoViewer by remember { mutableStateOf<PhotoViewerState?>(null) }
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

            val pendientesRevision = uiState.rows.count {
                it.reviewStatus == QuestionReviewStatus.PENDIENTE || it.reviewStatus == QuestionReviewStatus.SOLICITAR_INFO
            }
            if (uiState.rows.isNotEmpty()) {
                Text(
                    text = if (pendientesRevision == 0) {
                        "Revisión detallada completa: las ${uiState.rows.size} preguntas ya fueron validadas."
                    } else {
                        "Revisión detallada: faltan $pendientesRevision de ${uiState.rows.size} preguntas por validar."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.history.isNotEmpty()) {
                    item { DiagnosticHistorySection(entries = uiState.history) }
                }
                items(uiState.filteredRows, key = { it.question.codigo }) { row ->
                    var evidenciasExpanded by remember { mutableStateOf(false) }
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
                                TextButton(onClick = { evidenciasExpanded = !evidenciasExpanded }) {
                                    Text(
                                        if (evidenciasExpanded) {
                                            "Ocultar evidencias (${row.photos.size})"
                                        } else {
                                            "Ver evidencias (${row.photos.size})"
                                        }
                                    )
                                }
                                if (evidenciasExpanded) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        itemsIndexed(row.photos, key = { _, photo -> photo.id }) { index, photo ->
                                            AsyncImage(
                                                model = photo.uri,
                                                contentDescription = "Evidencia de ${row.question.codigo}, foto ${index + 1} de ${row.photos.size}, toca para ampliar",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { expandedPhotoViewer = PhotoViewerState(row.photos, index) }
                                            )
                                        }
                                    }
                                }
                            }
                            QuestionReviewBlock(
                                status = row.reviewStatus,
                                comentario = row.reviewComentario,
                                onStatusChange = { viewModel.setQuestionReviewStatus(row.question.codigo, it) },
                                onComentarioChange = { viewModel.setQuestionReviewComentario(row.question.codigo, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    expandedPhotoViewer?.let { state ->
        PhotoPagerDialog(state = state, onDismiss = { expandedPhotoViewer = null })
    }

    exportError?.let { message ->
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

/**
 * Visor de evidencias a pantalla completa: permite deslizar entre todas las
 * fotografías de una misma pregunta sin cerrar el visor.
 */
@Composable
private fun PhotoPagerDialog(state: PhotoViewerState, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = state.initialIndex) { state.photos.size }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = state.photos[page].uri,
                    contentDescription = "Foto de evidencia ${page + 1} de ${state.photos.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            if (state.photos.size > 1) {
                Text(
                    "${pagerState.currentPage + 1} / ${state.photos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }
}

private fun questionReviewStatusEmoji(status: QuestionReviewStatus) = when (status) {
    QuestionReviewStatus.APROBADO -> "✅"
    QuestionReviewStatus.PENDIENTE -> "🟡"
    QuestionReviewStatus.NO_APLICA -> "⚪"
    QuestionReviewStatus.SOLICITAR_INFO -> "🔵"
    QuestionReviewStatus.NO_CUMPLE -> "❌"
}

private fun questionReviewStatusColor(status: QuestionReviewStatus): Color = when (status) {
    QuestionReviewStatus.APROBADO -> EstadoAprobado
    QuestionReviewStatus.PENDIENTE -> EstadoPendiente
    QuestionReviewStatus.NO_APLICA -> EstadoNoAplica
    QuestionReviewStatus.SOLICITAR_INFO -> EstadoInfoRequerida
    QuestionReviewStatus.NO_CUMPLE -> EstadoNoCumple
}

/**
 * Bloque de validación exclusivo del administrador para una pregunta
 * individual: un solo botón "Evaluar pregunta" despliega un menú con las 5
 * opciones (una a la vez). "Solicitar información" y "No cumple" muestran un
 * campo de comentario debajo, visible para el cliente.
 */
@Composable
private fun QuestionReviewBlock(
    status: QuestionReviewStatus,
    comentario: String,
    onStatusChange: (QuestionReviewStatus) -> Unit,
    onComentarioChange: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                OutlinedButton(onClick = { menuExpanded = true }) {
                    Text("Evaluar pregunta")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    QuestionReviewStatus.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${questionReviewStatusEmoji(option)} ${option.label}") },
                            onClick = {
                                onStatusChange(option)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
            Chip(label = "${questionReviewStatusEmoji(status)} ${status.label}", color = questionReviewStatusColor(status))
        }
        if (status == QuestionReviewStatus.SOLICITAR_INFO || status == QuestionReviewStatus.NO_CUMPLE) {
            OutlinedTextField(
                value = comentario,
                onValueChange = onComentarioChange,
                label = {
                    Text(
                        if (status == QuestionReviewStatus.SOLICITAR_INFO) {
                            "¿Qué información o fotografía adicional se necesita?"
                        } else {
                            "Motivo por el que no cumple"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
