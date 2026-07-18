package com.example.todoaccesible.ui.cliente.diagnostic.new

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.BigTouchOutlinedButton
import com.example.todoaccesible.core.designsystem.ConfirmDialog
import com.example.todoaccesible.core.designsystem.PhotoPickerRow
import com.example.todoaccesible.core.designsystem.SectionPills
import com.example.todoaccesible.core.designsystem.answerValueColor
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito

@Composable
fun QuestionnaireScreen(
    viewModel: QuestionnaireViewModel,
    onNavigateBack: () -> Unit,
    onSubmitted: (Long) -> Unit,
    diagnosticId: Long
) {
    val uiState by viewModel.uiState.collectAsState()

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
            SectionPills(
                sections = uiState.sections,
                currentSectionId = question.seccionId,
                onSectionSelected = { viewModel.jumpToSection(it.id) }
            )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerOptionButton("AP", AnswerValue.APROBADO, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
                    AnswerOptionButton("P", AnswerValue.PENDIENTE, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
                    AnswerOptionButton("NC", AnswerValue.NO_CUMPLE, uiState.currentAnswerValue, viewModel::selectAnswer, Modifier.weight(1f))
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
                    text = if (uiState.isLastQuestion) "Finalizar" else "Siguiente",
                    enabled = uiState.canAdvance,
                    onClick = { if (uiState.isLastQuestion) viewModel.requestSubmit() else viewModel.nextQuestion() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (uiState.showSubmitConfirm) {
        ConfirmDialog(
            title = "Enviar cuestionario",
            message = "¿Enviar el diagnóstico para revisión? Ya no podrás editar las respuestas.",
            confirmLabel = "Enviar",
            onConfirm = { viewModel.confirmSubmit { onSubmitted(diagnosticId) } },
            onDismiss = viewModel::dismissSubmit
        )
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
