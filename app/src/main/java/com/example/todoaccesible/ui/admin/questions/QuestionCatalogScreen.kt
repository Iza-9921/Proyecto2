package com.example.todoaccesible.ui.admin.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy

@Composable
fun QuestionCatalogScreen(viewModel: QuestionCatalogViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val sectionNameById = remember(uiState.sections) { uiState.sections.associate { it.id to it.nombre } }

    Scaffold(topBar = { TopAppBar(title = { Text("Catálogo de preguntas") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.questions, key = { it.codigo }) { question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.startEditing(question) }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(question.codigo, style = MaterialTheme.typography.labelLarge)
                            Chip(
                                label = if (question.credito == Credito.REQUIRED) "Required" else "Plus",
                                color = if (question.credito == Credito.REQUIRED) RequiredNavy else PlusFuchsia
                            )
                        }
                        Text(question.concepto, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Sección: ${sectionNameById[question.seccionId] ?: question.seccionId} · Admite foto: ${if (question.admiteFoto) "Sí" else "No"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    uiState.editing?.let { question ->
        EditQuestionDialog(question = question, onSave = viewModel::saveEditing, onDismiss = viewModel::cancelEditing)
    }
}

@Composable
private fun EditQuestionDialog(
    question: QuestionEntity,
    onSave: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var concepto by remember(question.codigo) { mutableStateOf(question.concepto) }
    var admiteFoto by remember(question.codigo) { mutableStateOf(question.admiteFoto) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar pregunta ${question.codigo}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = concepto, onValueChange = { concepto = it }, label = { Text("Concepto") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = admiteFoto, onCheckedChange = { admiteFoto = it })
                    Text("Admite foto")
                }
            }
        },
        confirmButton = {
            BigTouchButton(text = "Guardar", onClick = { onSave(concepto, admiteFoto) })
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
