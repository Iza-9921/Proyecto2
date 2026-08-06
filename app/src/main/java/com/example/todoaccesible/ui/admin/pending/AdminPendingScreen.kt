package com.example.todoaccesible.ui.admin.pending

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.DiagnosticStatusChip
import com.example.todoaccesible.core.designsystem.NivelChip
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.DiagnosticStatus

@Composable
fun AdminPendingScreen(
    viewModel: AdminPendingViewModel,
    onOpenReview: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("Diagnósticos") }) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Kanban") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Tabla") })
            }

            if (tabIndex == 0) {
                KanbanBoard(diagnostics = uiState.filtered, onOpenReview = onOpenReview, onMove = viewModel::moveToColumn)
            } else {
                TableView(
                    uiState = uiState,
                    onQueryChange = viewModel::setQuery,
                    onFilterChange = viewModel::setFilterEstado,
                    onOpenReview = onOpenReview
                )
            }
        }
    }
}

/**
 * Todas las columnas visibles a la vez en una fila con scroll horizontal
 * (en vez del `HorizontalPager` de una columna a la vez que había antes),
 * para que una tarjeta se pueda arrastrar físicamente de una columna a
 * otra, igual que `KanbanBoard.jsx` en la web. Mantén presionada una
 * tarjeta y arrástrala sobre otra columna para soltarla ahí.
 */
@Composable
private fun KanbanBoard(
    diagnostics: List<DiagnosticEntity>,
    onOpenReview: (Long) -> Unit,
    onMove: (Long, KanbanColumn) -> Unit
) {
    val columns = KanbanColumn.entries
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val cardBounds = remember { mutableStateMapOf<Long, Rect>() }
    val columnBounds = remember { mutableStateMapOf<KanbanColumn, Rect>() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        columns.forEach { column ->
            val columnDiagnostics = diagnostics.filter { it.estado.kanbanColumn() == column }
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .onGloballyPositioned { coords -> columnBounds[column] = coords.boundsInRoot() }
            ) {
                Text(
                    "${column.label} (${columnDiagnostics.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(columnDiagnostics, key = { it.id }) { diagnostic ->
                        val isDragging = draggedId == diagnostic.id
                        DiagnosticKanbanCard(
                            diagnostic = diagnostic,
                            onClick = { onOpenReview(diagnostic.id) },
                            modifier = Modifier
                                .onGloballyPositioned { coords -> cardBounds[diagnostic.id] = coords.boundsInRoot() }
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        shadowElevation = 12f
                                        alpha = 0.92f
                                    }
                                }
                                .pointerInput(diagnostic.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedId = diagnostic.id
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            dragOffset += delta
                                        },
                                        onDragEnd = {
                                            val bounds = cardBounds[diagnostic.id]
                                            if (bounds != null) {
                                                val puntoSoltado = bounds.center + dragOffset
                                                val columnaDestino = columnBounds.entries.firstOrNull { (_, rect) -> rect.contains(puntoSoltado) }?.key
                                                if (columnaDestino != null && columnaDestino != column) {
                                                    onMove(diagnostic.id, columnaDestino)
                                                }
                                            }
                                            draggedId = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = { draggedId = null; dragOffset = Offset.Zero }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticKanbanCard(diagnostic: DiagnosticEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(diagnostic.projectName, style = MaterialTheme.typography.titleMedium)
            Text(diagnostic.ubicacion, style = MaterialTheme.typography.bodyMedium)
            if (diagnostic.estado == DiagnosticStatus.RECHAZADO) {
                Text("Rechazado", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
            diagnostic.nivel?.let { NivelChip(it) }
        }
    }
}

@Composable
private fun TableView(
    uiState: AdminPendingUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (DiagnosticStatus?) -> Unit,
    onOpenReview: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = { Text("Buscar por proyecto o cliente") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = uiState.filterEstado == null, onClick = { onFilterChange(null) }, label = { Text("Todos") })
            DiagnosticStatus.entries.filter { it != DiagnosticStatus.BORRADOR }.forEach { estado ->
                FilterChip(
                    selected = uiState.filterEstado == estado,
                    onClick = { onFilterChange(estado) },
                    label = { Text(estado.label) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filtered, key = { it.id }) { diagnostic ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpenReview(diagnostic.id) }) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(diagnostic.projectName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            uiState.usersById[diagnostic.clienteId]?.nombre ?: "Cliente",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiagnosticStatusChip(status = diagnostic.estado)
                            diagnostic.nivel?.let { NivelChip(it) }
                        }
                    }
                }
            }
        }
    }
}
