package com.example.todoaccesible.core.designsystem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.todoaccesible.data.local.seed.MexicoLocations
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Categorías de inmueble disponibles para el registro del proyecto; debe
 * coincidir exactamente con `TIPOS_INMUEBLE` en `categoriasMock.js` de la
 * web y con [com.example.todoaccesible.data.repository.impl.TipoCuestionarioRepositoryImpl.defaultTipos]
 * porque cada nombre es también la clave del catálogo de preguntas de ese tipo.
 */
val TiposDeInmueble: List<String> = listOf(
    "Edificio de oficinas",
    "Comercio",
    "Vivienda unifamiliar",
    "Edificio residencial",
    "Espacio público",
    "Local comercial",
    "Otro"
)

/**
 * Campos de datos de la empresa/inmueble a evaluar (nombre, cliente, teléfono,
 * dirección, estado/ciudad, tipo de inmueble, fecha de evaluación y
 * responsable). Se reutiliza tanto en el Paso 2 del registro de cuenta como
 * en "Datos del proyecto" al capturar un diagnóstico adicional.
 */
@Composable
fun EmpresaInfoFields(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    clienteNombre: String,
    onClienteNombreChange: (String) -> Unit,
    telefono: String,
    onTelefonoChange: (String) -> Unit,
    ubicacion: String,
    onUbicacionChange: (String) -> Unit,
    entidadFederativa: String,
    onEntidadFederativaChange: (String) -> Unit,
    ciudad: String,
    onCiudadChange: (String) -> Unit,
    tipoInmueble: String,
    onTipoInmuebleChange: (String) -> Unit,
    fechaEvaluacion: Long?,
    onFechaEvaluacionChange: (Long) -> Unit,
    responsable: String,
    onResponsableChange: (String) -> Unit,
    revision: String,
    onRevisionChange: (String) -> Unit,
    logoEmpresaUri: String?,
    onLogoEmpresaChange: (String?) -> Unit,
    projectNameLabel: String = "Nombre de la empresa",
    showRevision: Boolean = true,
    /** Cuando es `true`, el tipo de inmueble se muestra de solo lectura (ya asignado por el admin al activar la cuenta). */
    tipoInmuebleReadOnly: Boolean = false
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var otroTipoInmueble by remember { mutableStateOf(tipoInmueble.isNotBlank() && tipoInmueble !in TiposDeInmueble) }

    OutlinedTextField(
        value = projectName,
        onValueChange = onProjectNameChange,
        label = { Text(projectNameLabel) },
        placeholder = { Text("Ej: Edificio Corporativo Norte") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    LogoEmpresaPicker(logoUri = logoEmpresaUri, onLogoChange = onLogoEmpresaChange)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = clienteNombre,
            onValueChange = onClienteNombreChange,
            label = { Text("Cliente") },
            placeholder = { Text("Nombre del cliente") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = telefono,
            onValueChange = onTelefonoChange,
            label = { Text("Número telefónico") },
            placeholder = { Text("Ej: 55 1234 5678") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = ubicacion,
        onValueChange = onUbicacionChange,
        label = { Text("Dirección") },
        placeholder = { Text("Ej: Av. Principal 1234") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownField(
            label = "Estado",
            options = MexicoLocations.estados,
            selected = entidadFederativa,
            onSelected = onEntidadFederativaChange,
            placeholder = "Selecciona un estado",
            modifier = Modifier.weight(1f)
        )
        DropdownField(
            label = "Ciudad",
            options = MexicoLocations.ciudadesDe(entidadFederativa),
            selected = ciudad,
            onSelected = onCiudadChange,
            enabled = entidadFederativa.isNotBlank(),
            placeholder = "Selecciona una ciudad",
            modifier = Modifier.weight(1f)
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (tipoInmuebleReadOnly) {
            OutlinedTextField(
                value = tipoInmueble,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Tipo de inmueble") },
                supportingText = { Text("Asignado por el administrador") },
                modifier = Modifier.weight(1f)
            )
        } else {
            DropdownField(
                label = "Tipo de inmueble",
                options = TiposDeInmueble,
                selected = if (otroTipoInmueble) "Otro" else tipoInmueble,
                onSelected = { option ->
                    if (option == "Otro") {
                        otroTipoInmueble = true
                        onTipoInmuebleChange("")
                    } else {
                        otroTipoInmueble = false
                        onTipoInmuebleChange(option)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = fechaEvaluacion?.let { SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha de evaluación") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha")
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
    if (!tipoInmuebleReadOnly && otroTipoInmueble) {
        OutlinedTextField(
            value = tipoInmueble,
            onValueChange = onTipoInmuebleChange,
            label = { Text("Especifica el tipo de inmueble") },
            placeholder = { Text("Ej: Gimnasio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    OutlinedTextField(
        value = responsable,
        onValueChange = onResponsableChange,
        label = { Text("Responsable del levantamiento") },
        placeholder = { Text("Nombre del responsable") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (showRevision) {
        OutlinedTextField(
            value = revision,
            onValueChange = onRevisionChange,
            label = { Text("Revisión") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaEvaluacion)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onFechaEvaluacionChange)
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Selector del logotipo de la empresa del cliente (opcional): se guarda como
 * URI de contenido (igual que las fotos de evidencia) y se dibuja en el
 * scorecard PDF junto al encabezado.
 */
@Composable
private fun LogoEmpresaPicker(logoUri: String?, onLogoChange: (String?) -> Unit) {
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onLogoChange(it.toString()) } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logoUri != null) {
            AsyncImage(
                model = logoUri,
                contentDescription = "Logotipo de la empresa",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        OutlinedButton(
            onClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(if (logoUri != null) "Cambiar logotipo" else "Subir logotipo de la empresa", modifier = Modifier.padding(start = 8.dp))
        }
        if (logoUri != null) {
            IconButton(onClick = { onLogoChange(null) }) {
                Icon(Icons.Filled.Close, contentDescription = "Quitar logotipo", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
