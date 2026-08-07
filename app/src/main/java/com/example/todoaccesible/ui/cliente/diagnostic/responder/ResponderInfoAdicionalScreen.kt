package com.example.todoaccesible.ui.cliente.diagnostic.responder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.designsystem.PhotoPickerRow
import com.example.todoaccesible.core.designsystem.answerValueColor
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito

@Composable
fun ResponderInfoAdicionalScreen(
    viewModel: ResponderInfoAdicionalViewModel,
    onNavigateBack: () -> Unit,
    onNotAllowed: () -> Unit,
    onReenviado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loading, uiState.allowed) {
        if (!uiState.loading && !uiState.allowed) onNotAllowed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Información solicitada") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (!uiState.allowed) return@Scaffold

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "El administrador necesita más información en estas preguntas antes de continuar la revisión.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(uiState.preguntas, key = { it.question.codigo }) { flagged ->
                    PreguntaFlaggedCard(
                        flagged = flagged,
                        valor = uiState.answersByCode[flagged.question.codigo]?.valor,
                        comentario = uiState.answersByCode[flagged.question.codigo]?.comentario.orEmpty(),
                        photos = uiState.photosByCode[flagged.question.codigo].orEmpty(),
                        onSelectAnswer = { viewModel.selectAnswer(flagged.question.codigo, it) },
                        onComentarioChange = { viewModel.onComentarioChange(flagged.question.codigo, it) },
                        onPhotoAdded = { viewModel.onPhotoAdded(flagged.question.codigo, it) },
                        onDeletePhoto = { viewModel.deletePhoto(it) }
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                BigTouchButton(
                    text = if (uiState.submitting) "Enviando..." else "Reenviar respuestas",
                    enabled = uiState.puedeReenviar,
                    onClick = { viewModel.reenviar(onReenviado) }
                )
            }
        }
    }
}

@Composable
private fun PreguntaFlaggedCard(
    flagged: PreguntaFlagged,
    valor: AnswerValue?,
    comentario: String,
    photos: List<com.example.todoaccesible.core.designsystem.PhotoItem>,
    onSelectAnswer: (AnswerValue) -> Unit,
    onComentarioChange: (String) -> Unit,
    onPhotoAdded: (android.net.Uri) -> Unit,
    onDeletePhoto: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(flagged.question.codigo, style = MaterialTheme.typography.labelLarge)
                Chip(
                    label = if (flagged.question.credito == Credito.REQUIRED) "Required" else "Plus",
                    color = if (flagged.question.credito == Credito.REQUIRED) RequiredNavy else PlusFuchsia
                )
            }
            Text(flagged.question.concepto, style = MaterialTheme.typography.titleMedium)
            if (flagged.comentarioAdmin.isNotBlank()) {
                Text(
                    "Observación del administrador: ${flagged.comentarioAdmin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RespuestaOpcionButton("AP", AnswerValue.APROBADO, valor, onSelectAnswer, Modifier.weight(1f))
                RespuestaOpcionButton("P", AnswerValue.PENDIENTE, valor, onSelectAnswer, Modifier.weight(1f))
                RespuestaOpcionButton("NC", AnswerValue.NO_CUMPLE, valor, onSelectAnswer, Modifier.weight(1f))
                RespuestaOpcionButton("NA", AnswerValue.NO_APLICA, valor, onSelectAnswer, Modifier.weight(1f))
            }
            OutlinedTextField(
                value = comentario,
                onValueChange = onComentarioChange,
                label = { Text("Comentario / observación (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            if (flagged.question.admiteFoto) {
                Text("Evidencia fotográfica", style = MaterialTheme.typography.labelLarge)
                PhotoPickerRow(
                    photos = photos,
                    onPhotoAdded = onPhotoAdded,
                    onRequestDelete = { onDeletePhoto(it.id) }
                )
            }
        }
    }
}

@Composable
private fun RespuestaOpcionButton(
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
        modifier = modifier.semantics { contentDescription = if (isSelected) "$label, seleccionado" else label }
    ) {
        Text(label, maxLines = 1)
    }
}
