package com.example.todoaccesible.ui.cliente.diagnostic.new

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.BigTouchOutlinedButton
import com.example.todoaccesible.core.designsystem.ConfirmDialog
import com.example.todoaccesible.core.designsystem.PhotoPickerRow
import com.example.todoaccesible.core.designsystem.answerValueColor
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito

@Composable
fun QuestionnaireScreen(
    viewModel: QuestionnaireViewModel,
    onNavigateBack: () -> Unit,
    onGoToSummary: (Long) -> Unit,
    diagnosticId: Long
) {
    val uiState by viewModel.uiState.collectAsState()
    val sectionCompletion by viewModel.sectionCompletion.collectAsState()
    val submitBlockedMessage by viewModel.submitBlockedMessage.collectAsState()
    val showQuestionList by viewModel.showQuestionList.collectAsState()
    val currentSectionQuestions by viewModel.currentSectionQuestions.collectAsState()
    var showReferenceImage by remember { mutableStateOf(false) }
    var showSectionList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pregunta ${uiState.currentIndex + 1} de ${uiState.totalQuestions}") },
                actions = {
                    TextButton(onClick = viewModel::requestDiscard) {
                        Text("Descartar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.loading || uiState.currentQuestion == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val question = uiState.currentQuestion!!

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { showSectionList = true }) {
                    Text("Categorías")
                }
                TextButton(onClick = viewModel::openQuestionList) {
                    Text("Ver preguntas")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (question.credito == Credito.REQUIRED) "Required" else "Plus",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = question.concepto, style = MaterialTheme.typography.titleLarge)

                question.imagenEjemplo?.let {
                    OutlinedButton(onClick = { showReferenceImage = true }) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Ver imagen de referencia")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerOptionButton("AP", AnswerValue.APROBADO, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
                    AnswerOptionButton("P", AnswerValue.PENDIENTE, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
                    AnswerOptionButton("NC", AnswerValue.NO_CUMPLE, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
                    AnswerOptionButton("NA", AnswerValue.NO_APLICA, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
                }

                if (!uiState.canAdvance) {
                    Text(
                        text = "Selecciona una respuesta para poder continuar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = uiState.currentComentario,
                    onValueChange = viewModel::onComentarioChange,
                    label = { Text("Comentario / observación (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (question.admiteFoto) {
                    Text("Evidencia fotográfica", style = MaterialTheme.typography.labelLarge)
                    PhotoPickerRow(
                        photos = uiState.currentPhotos,
                        onPhotoAdded = viewModel::onPhotoAdded,
                        onRequestDelete = viewModel::requestDeletePhoto
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigTouchOutlinedButton(
                    text = "Anterior",
                    enabled = !uiState.isFirstQuestion,
                    onClick = viewModel::previousQuestion,
                    modifier = Modifier.weight(1f)
                )
                BigTouchButton(
                    text = if (uiState.isLastQuestion) "Ir al resumen" else "Siguiente",
                    enabled = uiState.canAdvance,
                    onClick = {
                        if (uiState.isLastQuestion) {
                            viewModel.goToSummary { onGoToSummary(diagnosticId) }
                        } else {
                            viewModel.nextQuestion()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (uiState.showDiscardConfirm) {
        ConfirmDialog(
            title = "Descartar borrador",
            message = "Se perderán todas las respuestas capturadas en este diagnóstico. Esta acción no se puede deshacer.",
            confirmLabel = "Descartar",
            isDestructive = true,
            onConfirm = { viewModel.confirmDiscard(onNavigateBack) },
            onDismiss = viewModel::dismissDiscard
        )
    }

    uiState.photoPendingDelete?.let {
        ConfirmDialog(
            title = "Eliminar foto",
            message = "¿Eliminar esta foto de evidencia?",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onConfirm = viewModel::confirmDeletePhoto,
            onDismiss = viewModel::dismissDeletePhoto
        )
    }

    if (showReferenceImage) {
        uiState.currentQuestion?.imagenEjemplo?.let { uri ->
            ReferenceImageDialog(uri = uri, onDismiss = { showReferenceImage = false })
        }
    }

    submitBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSubmitBlocked,
            title = { Text("Preguntas pendientes") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSubmitBlocked) { Text("Entendido") }
            }
        )
    }

    if (showSectionList) {
        ModalBottomSheet(onDismissRequest = { showSectionList = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.sections, key = { it.id }) { section ->
                    val selected = section.id == uiState.currentQuestion?.seccionId
                    val completada = sectionCompletion[section.id] == true
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.jumpToSection(section.id)
                                showSectionList = false
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "${if (completada) "🟢" else "🟡"} ${section.id}. ${section.nombre}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showQuestionList) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissQuestionList) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(currentSectionQuestions, key = { it.index }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.goToQuestion(item.index) }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "${if (item.answered) "🟢" else "🟡"} ${item.numero}. ${item.question.concepto}",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (item.answered) "Contestada" else "Pendiente",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Imagen de referencia que el admin subió para esa pregunta (guía sobre qué evaluar/qué foto tomar), a pantalla completa. */
@Composable
private fun ReferenceImageDialog(uri: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = "Imagen de referencia de la pregunta",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

@Composable
private fun AnswerOptionButton(
    label: String,
    value: AnswerValue,
    selected: AnswerValue?,
    onSelect: (AnswerValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selected == value
    val color = answerValueColor(value)
    OutlinedButton(
        onClick = { onSelect(value) },
        colors = if (isSelected) {
            ButtonDefaults.outlinedButtonColors(containerColor = color, contentColor = androidx.compose.ui.graphics.Color.White)
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = modifier
            .height(56.dp)
            .semantics { contentDescription = if (isSelected) "$label, seleccionado" else label }
    ) {
        Text(label, maxLines = 1)
    }
}
