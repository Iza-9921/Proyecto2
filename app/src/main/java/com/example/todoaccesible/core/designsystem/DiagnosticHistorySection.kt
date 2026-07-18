package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.data.local.entities.DiagnosticHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** RF-20: entrada de historial ya resuelta (nombre del revisor) para mostrar en pantalla. */
data class HistoryEntryUi(
    val entry: DiagnosticHistoryEntity,
    val reviewerName: String
)

/**
 * Línea de tiempo de cambios de estado de un diagnóstico (envío, revisión,
 * validación). Reutilizada tanto en la pantalla de revisión del admin como en
 * el detalle del cliente, siguiendo el mismo patrón visual de tarjeta que
 * `NotificationsScreen`.
 */
@Composable
fun DiagnosticHistorySection(entries: List<HistoryEntryUi>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Historial", style = MaterialTheme.typography.titleMedium)
        entries.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagnosticStatusChip(status = item.entry.newStatus)
                    }
                    Text(
                        "Por: ${item.reviewerName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.entry.comentario.isNotBlank()) {
                        Text(item.entry.comentario, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX")).format(Date(item.entry.fecha)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
