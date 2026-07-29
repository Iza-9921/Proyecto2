package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.data.local.entities.SectionEntity

/**
 * Pills horizontales para saltar libremente a la primera pregunta de
 * cualquier sección dentro del cuestionario del cliente (sin perder las
 * respuestas ya capturadas en otras secciones). `sectionCompletion` marca,
 * por id de sección, si ya todas sus preguntas tienen respuesta (🟢) o si
 * todavía falta alguna (🟡).
 */
@Composable
fun SectionPills(
    sections: List<SectionEntity>,
    currentSectionId: String,
    onSectionSelected: (SectionEntity) -> Unit,
    modifier: Modifier = Modifier,
    sectionCompletion: Map<String, Boolean> = emptyMap()
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        items(sections, key = { it.id }) { section ->
            val selected = section.id == currentSectionId
            val completada = sectionCompletion[section.id] == true
            Surface(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(50),
                onClick = { onSectionSelected(section) }
            ) {
                Text(
                    text = "${if (completada) "🟢" else "🟡"} ${section.id}. ${section.nombre}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
