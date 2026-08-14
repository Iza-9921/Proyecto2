package com.example.todoaccesible.ui.admin.questions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.todoaccesible.core.designsystem.DropdownField
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito

@Composable
fun QuestionCatalogScreen(viewModel: QuestionCatalogViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Cuestionarios") }) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        label = "Cuestionario",
                        options = uiState.tipos,
                        selected = uiState.selectedTipo,
                        onSelected = viewModel::selectTipo,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.openDialog(QuestionCatalogDialog.CreateTipo) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear nuevo cuestionario")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { viewModel.openDialog(QuestionCatalogDialog.RenombrarTipo) }) {
                        Text("Editar nombre")
                    }
                    TextButton(onClick = { viewModel.openDialog(QuestionCatalogDialog.DuplicarTipo) }) {
                        Text("Duplicar este cuestionario")
                    }
                    TextButton(
                        onClick = { viewModel.openDialog(QuestionCatalogDialog.DeleteTipo) },
                        enabled = uiState.puedeEliminarTipo
                    ) {
                        Text("Eliminar este cuestionario", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.sections, key = { it.id }) { section ->
                    SeccionCard(
                        section = section,
                        preguntas = uiState.preguntasDe(section.id),
                        expanded = section.id in uiState.expandedSections,
                        onToggle = { viewModel.toggleSection(section.id) },
                        onEditSection = { viewModel.openDialog(QuestionCatalogDialog.EditSection(section)) },
                        onDeleteSection = { viewModel.openDialog(QuestionCatalogDialog.DeleteSection(section)) },
                        onEditQuestion = { viewModel.openDialog(QuestionCatalogDialog.EditQuestion(it)) },
                        onDeleteQuestion = { viewModel.openDialog(QuestionCatalogDialog.DeleteQuestion(it)) },
                        onAddQuestion = { viewModel.openDialog(QuestionCatalogDialog.AddQuestion(section.id)) },
                        onElegirPreguntas = { viewModel.openDialog(QuestionCatalogDialog.ElegirPreguntas(section.id)) }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.openDialog(QuestionCatalogDialog.AddSection) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Agregar sección", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    when (val dialog = uiState.dialog) {
        is QuestionCatalogDialog.CreateTipo -> TextInputDialog(
            title = "Crear nuevo cuestionario",
            label = "Nombre del cuestionario",
            onConfirm = viewModel::confirmCreateTipo,
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.RenombrarTipo -> TextInputDialog(
            title = "Editar nombre del cuestionario",
            label = "Nuevo nombre",
            initialValue = uiState.selectedTipo,
            onConfirm = viewModel::confirmRenombrarTipo,
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.DuplicarTipo -> TextInputDialog(
            title = "Duplicar cuestionario",
            label = "Nombre del nuevo cuestionario",
            initialValue = "${uiState.selectedTipo} (copia)",
            onConfirm = viewModel::confirmDuplicarTipo,
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.ElegirPreguntas -> BancoPreguntasDialog(
            cargar = viewModel::cargarBancoPreguntas,
            onConfirm = { seleccionadas -> viewModel.confirmAgregarDesdeBanco(dialog.seccionId, seleccionadas) },
            onDismiss = viewModel::closeDialog
        )
        // El botón que abría este diálogo ya no existe (no hay endpoint de backend para "restaurar
        // ejemplo"); se deja el branch solo para que el `when` siga siendo exhaustivo.
        is QuestionCatalogDialog.RestaurarEjemplo -> Unit
        is QuestionCatalogDialog.DeleteTipo -> ConfirmDialog(
            title = "Eliminar cuestionario",
            message = "Se eliminará \"${uiState.selectedTipo}\" junto con todas sus secciones y preguntas. Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onConfirm = viewModel::confirmDeleteTipo,
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.AddSection -> SeccionDialog(
            title = "Agregar sección",
            onConfirm = viewModel::confirmAddSection,
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.EditSection -> SeccionDialog(
            title = "Editar sección",
            icono = dialog.section.icono,
            tituloLargo = dialog.section.nombre,
            tituloCorto = dialog.section.tituloCorto,
            onConfirm = { icono, largo, corto -> viewModel.confirmEditSection(dialog.section.id, icono, largo, corto) },
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.DeleteSection -> ConfirmDialog(
            title = "Eliminar sección",
            message = "Se eliminará \"${dialog.section.nombre}\" junto con todas sus preguntas.",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onConfirm = { viewModel.confirmDeleteSection(dialog.section.id) },
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.AddQuestion -> PreguntaDialog(
            title = "Agregar pregunta",
            onConfirm = { concepto, credito, admiteFoto, _, _ ->
                viewModel.confirmAddQuestion(dialog.seccionId, concepto, credito, admiteFoto)
            },
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.EditQuestion -> PreguntaDialog(
            title = "Editar pregunta",
            concepto = dialog.question.concepto,
            credito = dialog.question.credito,
            admiteFoto = dialog.question.admiteFoto,
            descripcion = dialog.question.descripcion,
            imagenEjemplo = dialog.question.imagenEjemplo,
            onConfirm = { concepto, credito, admiteFoto, descripcion, imagenEjemplo ->
                viewModel.confirmEditQuestion(dialog.question.codigo, concepto, credito, admiteFoto, descripcion, imagenEjemplo)
            },
            onDismiss = viewModel::closeDialog
        )
        is QuestionCatalogDialog.DeleteQuestion -> ConfirmDialog(
            title = "Eliminar pregunta",
            message = "Se eliminará la pregunta \"${dialog.question.concepto}\".",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onConfirm = { viewModel.confirmDeleteQuestion(dialog.question.codigo) },
            onDismiss = viewModel::closeDialog
        )
        null -> Unit
    }
}

@Composable
private fun SeccionCard(
    section: SectionEntity,
    preguntas: List<QuestionEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditSection: () -> Unit,
    onDeleteSection: () -> Unit,
    onEditQuestion: (QuestionEntity) -> Unit,
    onDeleteQuestion: (QuestionEntity) -> Unit,
    onAddQuestion: () -> Unit,
    onElegirPreguntas: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onToggle) {
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (expanded) "Contraer" else "Expandir")
                    }
                    Column {
                        Text("${section.icono} ${section.nombre}".trim(), style = MaterialTheme.typography.titleMedium)
                        Text("${preguntas.size} preguntas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEditSection) { Icon(Icons.Filled.Edit, contentDescription = "Editar sección") }
                IconButton(onClick = onDeleteSection) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar sección", tint = MaterialTheme.colorScheme.error) }
            }

            if (expanded) {
                preguntas.forEachIndexed { index, pregunta ->
                    PreguntaRow(pregunta = pregunta, numero = index + 1, onEdit = { onEditQuestion(pregunta) }, onDelete = { onDeleteQuestion(pregunta) })
                    HorizontalDivider()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onAddQuestion) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Agregar pregunta", modifier = Modifier.padding(start = 4.dp))
                    }
                    TextButton(onClick = onElegirPreguntas) {
                        Text("Elegir preguntas")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreguntaRow(pregunta: QuestionEntity, numero: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    var verInstruccion by remember(pregunta.codigo) { mutableStateOf(false) }
    val tieneInstruccion = pregunta.descripcion.isNotBlank() || pregunta.imagenEjemplo != null

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(numero.toString(), style = MaterialTheme.typography.labelLarge)
                    Chip(
                        label = if (pregunta.credito == Credito.REQUIRED) "Required" else "Plus",
                        color = if (pregunta.credito == Credito.REQUIRED) RequiredNavy else PlusFuchsia
                    )
                    if (pregunta.admiteFoto) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Admite foto", modifier = Modifier.size(16.dp))
                    }
                }
                Text(pregunta.concepto, style = MaterialTheme.typography.bodyLarge)
                if (tieneInstruccion) {
                    TextButton(onClick = { verInstruccion = !verInstruccion }) {
                        Text(if (verInstruccion) "Ocultar instrucción" else "Ver cómo hacerlo")
                    }
                    if (verInstruccion) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (pregunta.descripcion.isNotBlank()) {
                                Text(pregunta.descripcion, style = MaterialTheme.typography.bodyMedium)
                            }
                            pregunta.imagenEjemplo?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Imagen de ejemplo",
                                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar pregunta") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar pregunta", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { BigTouchButton(text = "Guardar", onClick = { onConfirm(value) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SeccionDialog(
    title: String,
    icono: String = "",
    tituloLargo: String = "",
    tituloCorto: String = "",
    onConfirm: (icono: String, tituloLargo: String, tituloCorto: String) -> Unit,
    onDismiss: () -> Unit
) {
    var iconoValue by remember { mutableStateOf(icono) }
    var largoValue by remember { mutableStateOf(tituloLargo) }
    var cortoValue by remember { mutableStateOf(tituloCorto) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = iconoValue, onValueChange = { iconoValue = it }, label = { Text("Ícono (emoji, opcional)") }, singleLine = true)
                OutlinedTextField(value = largoValue, onValueChange = { largoValue = it }, label = { Text("Título largo") }, singleLine = true)
                OutlinedTextField(value = cortoValue, onValueChange = { cortoValue = it }, label = { Text("Título corto") }, singleLine = true)
            }
        },
        confirmButton = {
            BigTouchButton(text = "Guardar", onClick = { onConfirm(iconoValue, largoValue, cortoValue) })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun PreguntaDialog(
    title: String,
    concepto: String = "",
    credito: Credito = Credito.REQUIRED,
    admiteFoto: Boolean = true,
    descripcion: String = "",
    imagenEjemplo: String? = null,
    onConfirm: (concepto: String, credito: Credito, admiteFoto: Boolean, descripcion: String, imagenEjemplo: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var conceptoValue by remember { mutableStateOf(concepto) }
    var creditoValue by remember { mutableStateOf(credito) }
    var admiteFotoValue by remember { mutableStateOf(admiteFoto) }
    var descripcionValue by remember { mutableStateOf(descripcion) }
    var imagenValue by remember { mutableStateOf(imagenEjemplo) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { imagenValue = it.toString() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = conceptoValue, onValueChange = { conceptoValue = it }, label = { Text("Concepto") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = creditoValue == Credito.REQUIRED, onClick = { creditoValue = Credito.REQUIRED })
                    Text("Required", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = creditoValue == Credito.PLUS, onClick = { creditoValue = Credito.PLUS })
                    Text("Plus")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = admiteFotoValue, onCheckedChange = { admiteFotoValue = it })
                    Text("Admite foto")
                }
                OutlinedTextField(
                    value = descripcionValue,
                    onValueChange = { descripcionValue = it },
                    label = { Text("Instrucción / \"Ver cómo hacerlo\" (opcional)") }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    imagenValue?.let { uri ->
                        AsyncImage(model = uri, contentDescription = "Imagen de ejemplo", modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                    }
                    OutlinedButton(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(if (imagenValue != null) "Cambiar imagen" else "Imagen de ejemplo", modifier = Modifier.padding(start = 6.dp))
                    }
                    if (imagenValue != null) {
                        IconButton(onClick = { imagenValue = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar imagen", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            BigTouchButton(
                text = "Guardar",
                onClick = { onConfirm(conceptoValue, creditoValue, admiteFotoValue, descripcionValue, imagenValue) }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun BancoPreguntasDialog(
    cargar: suspend () -> List<QuestionEntity>,
    onConfirm: (List<QuestionEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    var cargando by remember { mutableStateOf(true) }
    var preguntas by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var busqueda by remember { mutableStateOf("") }
    var seleccionadas by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        preguntas = cargar()
        cargando = false
    }

    val filtradas = preguntas.filter { it.concepto.contains(busqueda, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir preguntas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar pregunta...") },
                    singleLine = true
                )
                when {
                    cargando -> Text("Cargando preguntas...")
                    filtradas.isEmpty() -> Text("No se encontraron preguntas.")
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filtradas, key = { "${it.tipo}-${it.codigo}" }) { pregunta ->
                            val marcada = pregunta.concepto in seleccionadas
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    seleccionadas = if (marcada) seleccionadas - pregunta.concepto else seleccionadas + pregunta.concepto
                                }
                            ) {
                                Checkbox(checked = marcada, onCheckedChange = null)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pregunta.concepto, style = MaterialTheme.typography.bodyMedium)
                                    Text(pregunta.tipo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BigTouchButton(
                text = "Agregar seleccionadas (${seleccionadas.size})",
                onClick = { onConfirm(preguntas.filter { it.concepto in seleccionadas }) }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
