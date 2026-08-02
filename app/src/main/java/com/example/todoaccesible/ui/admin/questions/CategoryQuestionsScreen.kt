package com.example.todoaccesible.ui.admin.questions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.designsystem.ConfirmDialog
import com.example.todoaccesible.core.theme.EstadoAprobado
import com.example.todoaccesible.core.theme.EstadoNoAplica
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito

/** Preguntas de una categoría (pantalla a la que se entra al tocar una categoría en "Gestión del cuestionario"). */
@Composable
fun CategoryQuestionsScreen(
    sectionId: String,
    viewModel: CategoryQuestionsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<QuestionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.categoryName.ifBlank { "Preguntas" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva pregunta")
            }
        }
    ) { innerPadding ->
        if (uiState.questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    "Esta categoría todavía no tiene preguntas. Usa el botón + para agregar la primera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.questions, key = { _, q -> q.codigo }) { index, question ->
                    QuestionCard(
                        question = question,
                        isFirst = index == 0,
                        isLast = index == uiState.questions.lastIndex,
                        onEdit = { editingQuestion = question },
                        onDelete = { pendingDelete = question },
                        onToggleActive = { viewModel.setActive(question, !question.activa) },
                        onMoveUp = { viewModel.moveUp(question.codigo) },
                        onMoveDown = { viewModel.moveDown(question.codigo) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        QuestionFormDialog(
            title = "Nueva pregunta",
            initial = null,
            defaultSeccionId = sectionId,
            sections = uiState.allSections,
            onConfirm = { seccionIdSel, texto, descripcion, credito, activa, imagenUri ->
                viewModel.createQuestion(seccionIdSel, texto, descripcion, credito, activa, imagenUri)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingQuestion?.let { question ->
        QuestionFormDialog(
            title = "Editar pregunta",
            initial = question,
            defaultSeccionId = sectionId,
            sections = uiState.allSections,
            onConfirm = { seccionIdSel, texto, descripcion, credito, activa, imagenUri ->
                viewModel.updateQuestion(
                    question.copy(
                        seccionId = seccionIdSel,
                        concepto = texto,
                        descripcion = descripcion,
                        credito = credito,
                        activa = activa,
                        imagenReferenciaUri = imagenUri
                    )
                )
                editingQuestion = null
            },
            onDismiss = { editingQuestion = null }
        )
    }

    pendingDelete?.let { question ->
        ConfirmDialog(
            title = "Eliminar pregunta",
            message = "¿Eliminar la pregunta \"${question.concepto}\"? Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteQuestion(question.codigo)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun QuestionCard(
    question: QuestionEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(
                    label = if (question.credito == Credito.REQUIRED) "Required" else "Plus",
                    color = if (question.credito == Credito.REQUIRED) RequiredNavy else PlusFuchsia
                )
                Chip(
                    label = if (question.activa) "Activa" else "Inactiva",
                    color = if (question.activa) EstadoAprobado else EstadoNoAplica
                )
                if (question.imagenReferenciaUri != null) {
                    Chip(label = "Con imagen de referencia", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(question.concepto, style = MaterialTheme.typography.bodyLarge)
            if (question.descripcion.isNotBlank()) {
                Text(
                    question.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            question.imagenReferenciaUri?.let { uri ->
                AsyncImage(
                    model = Uri.parse(uri),
                    contentDescription = "Imagen de referencia de la pregunta",
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir pregunta")
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar pregunta")
                    }
                    Text("Activa", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                    Switch(checked = question.activa, onCheckedChange = onToggleActive)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar pregunta")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar pregunta")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionFormDialog(
    title: String,
    initial: QuestionEntity?,
    defaultSeccionId: String,
    sections: List<SectionEntity>,
    onConfirm: (seccionId: String, texto: String, descripcion: String, credito: Credito, activa: Boolean, imagenUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var seccionId by remember { mutableStateOf(initial?.seccionId ?: defaultSeccionId) }
    var texto by remember { mutableStateOf(initial?.concepto ?: "") }
    var descripcion by remember { mutableStateOf(initial?.descripcion ?: "") }
    var credito by remember { mutableStateOf(initial?.credito ?: Credito.REQUIRED) }
    var activa by remember { mutableStateOf(initial?.activa ?: true) }
    var imagenUri by remember { mutableStateOf(initial?.imagenReferenciaUri?.let { Uri.parse(it) }) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) imagenUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    OutlinedButton(onClick = { categoryMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Categoría: " + (sections.find { it.id == seccionId }?.nombre ?: seccionId))
                    }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.nombre) },
                                onClick = { seccionId = section.id; categoryMenuExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Texto de la pregunta") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción o ayuda (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = { credito = Credito.REQUIRED }) {
                        Text(if (credito == Credito.REQUIRED) "● Required" else "○ Required")
                    }
                    TextButton(onClick = { credito = Credito.PLUS }) {
                        Text(if (credito == Credito.PLUS) "● Plus" else "○ Plus")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activa", modifier = Modifier.padding(end = 8.dp))
                    Switch(checked = activa, onCheckedChange = { activa = it })
                }
                Text("Imagen de referencia (opcional)", style = MaterialTheme.typography.labelLarge)
                val currentUri = imagenUri
                if (currentUri != null) {
                    AsyncImage(
                        model = currentUri,
                        contentDescription = "Vista previa de la imagen de referencia",
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Row {
                        TextButton(onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("Reemplazar") }
                        TextButton(onClick = { imagenUri = null }) { Text("Eliminar imagen") }
                    }
                } else {
                    OutlinedButton(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Seleccionar imagen")
                    }
                }
            }
        },
        confirmButton = {
            BigTouchButton(
                text = "Guardar",
                enabled = texto.isNotBlank(),
                onClick = { onConfirm(seccionId, texto, descripcion, credito, activa, imagenUri?.toString()) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
