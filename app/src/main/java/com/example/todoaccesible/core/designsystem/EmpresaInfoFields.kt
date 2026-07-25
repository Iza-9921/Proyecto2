package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.data.local.seed.MexicoLocations
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Categorías de inmueble disponibles para el registro del proyecto. */
val TiposDeInmueble: List<String> = listOf(
    "Edificio de oficinas",
    "Centro comercial",
    "Restaurante",
    "Hotel",
    "Hospital o clínica",
    "Escuela",
    "Vivienda",
    "Espacio público",
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
    projectNameLabel: String = "Nombre de la empresa",
    showRevision: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = projectName,
        onValueChange = onProjectNameChange,
        label = { Text(projectNameLabel) },
        placeholder = { Text("Ej: Edificio Corporativo Norte") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
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
        DropdownField(
            label = "Tipo de inmueble",
            options = TiposDeInmueble,
            selected = tipoInmueble,
            onSelected = onTipoInmuebleChange,
            modifier = Modifier.weight(1f)
        )
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
