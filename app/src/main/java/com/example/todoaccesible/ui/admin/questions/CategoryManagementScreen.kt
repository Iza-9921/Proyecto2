package com.example.todoaccesible.ui.admin.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchOutlinedButton
import com.example.todoaccesible.core.designsystem.Chip
import com.example.todoaccesible.core.designsystem.ConfirmDialog
import com.example.todoaccesible.core.theme.EstadoAprobado
import com.example.todoaccesible.core.theme.EstadoNoAplica
import com.example.todoaccesible.data.local.entities.SectionEntity

/** Pestaña "Categorías" del módulo Gestión del cuestionario: alta, edición, orden, activo/inactivo y borrado. */
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel,
    onOpenCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<SectionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<SectionEntity?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${uiState.categories.size} categorías", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Nueva categoría")
            }
        }

        if (uiState.categories.isEmpty() && !uiState.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay categorías. Crea la primera con el botón de arriba.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.categories) { index, row ->
                    CategoryCard(
                        row = row,
                        isFirst = index == 0,
                        isLast = index == uiState.categories.lastIndex,
                        onOpen = { onOpenCategory(row.section.id) },
                        onEdit = { editingCategory = row.section },
                        onDelete = { pendingDelete = row.section },
                        onToggleActive = { viewModel.setActive(row.section.id, !row.section.activa) },
                        onMoveUp = { viewModel.moveUp(row.section.id) },
                        onMoveDown = { viewModel.moveDown(row.section.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CategoryNameDialog(
            title = "Nueva categoría",
            initialValue = "",
            confirmLabel = "Crear",
            onConfirm = { nombre ->
                viewModel.createCategory(nombre)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingCategory?.let { category ->
        CategoryNameDialog(
            title = "Editar categoría",
            initialValue = category.nombre,
            confirmLabel = "Guardar",
            onConfirm = { nombre ->
                viewModel.renameCategory(category.id, nombre)
                editingCategory = null
            },
            onDismiss = { editingCategory = null }
        )
    }

    pendingDelete?.let { category ->
        ConfirmDialog(
            title = "Eliminar categoría",
            message = "¿Eliminar la categoría \"${category.nombre}\"? Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCategory(category.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    deleteBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlockedMessage,
            title = { Text("No se puede eliminar") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlockedMessage) { Text("Entendido") }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    row: CategoryRow,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val section = row.section
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${row.questionCount} " + (if (row.questionCount == 1) "pregunta" else "preguntas") + " · toca para ver",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Chip(
                    label = if (section.activa) "Activa" else "Inactiva",
                    color = if (section.activa) EstadoAprobado else EstadoNoAplica
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir categoría")
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar categoría")
                    }
                    Text("Activa", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                    Switch(checked = section.activa, onCheckedChange = onToggleActive)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar categoría")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar categoría")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la categoría") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            BigTouchOutlinedButton(
                text = confirmLabel,
                enabled = nombre.isNotBlank(),
                onClick = { onConfirm(nombre) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
