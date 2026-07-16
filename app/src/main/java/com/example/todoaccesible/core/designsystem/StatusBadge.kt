package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.theme.EstadoAprobado
import com.example.todoaccesible.core.theme.EstadoNoAplica
import com.example.todoaccesible.core.theme.EstadoNoCumple
import com.example.todoaccesible.core.theme.EstadoPendiente
import com.example.todoaccesible.core.theme.NivelEnProceso
import com.example.todoaccesible.core.theme.NivelMagenta
import com.example.todoaccesible.core.theme.NivelOro
import com.example.todoaccesible.core.theme.NivelPlata
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Nivel

@Composable
fun Chip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

fun answerValueColor(value: AnswerValue?): Color = when (value) {
    AnswerValue.APROBADO -> EstadoAprobado
    AnswerValue.PENDIENTE -> EstadoPendiente
    AnswerValue.NO_CUMPLE -> EstadoNoCumple
    AnswerValue.NO_APLICA -> EstadoNoAplica
    null -> EstadoNoAplica
}

fun nivelColor(nivel: Nivel): Color = when (nivel) {
    Nivel.EN_PROCESO -> NivelEnProceso
    Nivel.PLATA -> NivelPlata
    Nivel.ORO -> NivelOro
    Nivel.MAGENTA -> NivelMagenta
}

@Composable
fun AnswerValueChip(value: AnswerValue?, modifier: Modifier = Modifier) {
    val label = value?.short ?: "—"
    Chip(label = label, color = answerValueColor(value), modifier = modifier)
}

@Composable
fun NivelChip(nivel: Nivel, modifier: Modifier = Modifier) {
    Chip(label = nivel.label, color = nivelColor(nivel), modifier = modifier)
}

@Composable
fun DiagnosticStatusChip(status: DiagnosticStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        DiagnosticStatus.BORRADOR -> EstadoNoAplica
        DiagnosticStatus.PENDIENTE -> EstadoPendiente
        DiagnosticStatus.EN_REVISION -> NivelOro
        DiagnosticStatus.INFO_REQUERIDA -> EstadoPendiente
        DiagnosticStatus.RECHAZADO -> EstadoNoCumple
        DiagnosticStatus.VALIDADO -> EstadoAprobado
    }
    Chip(label = status.label, color = color, modifier = modifier)
}
