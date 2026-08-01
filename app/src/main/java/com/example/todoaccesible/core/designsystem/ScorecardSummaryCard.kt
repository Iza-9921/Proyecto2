package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.theme.PlusFuchsia
import com.example.todoaccesible.core.theme.RequiredNavy
import com.example.todoaccesible.data.model.Nivel
import com.example.todoaccesible.domain.scoring.CreditScore
import com.example.todoaccesible.domain.scoring.SectionScore

@Composable
fun CreditBar(label: String, score: CreditScore, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("${score.aprobados}/${score.total}  ·  ${score.pct}%", style = MaterialTheme.typography.labelLarge)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (score.pct / 100f).coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ScorecardHeaderCard(
    nivel: Nivel,
    required: CreditScore,
    plus: CreditScore,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nivel alcanzado", style = MaterialTheme.typography.titleMedium)
                NivelChip(nivel)
            }
            CreditBar(label = "Required", score = required, color = RequiredNavy)
            CreditBar(label = "Plus", score = plus, color = PlusFuchsia)
        }
    }
}

@Composable
fun SectionScoreRow(section: SectionScore, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "${section.seccionId}. ${section.seccionNombre}",
                style = MaterialTheme.typography.titleMedium
            )
            CreditBar(label = "Required", score = section.required, color = RequiredNavy)
            CreditBar(label = "Plus", score = section.plus, color = PlusFuchsia)
        }
    }
}

/** Celda compacta para la grilla de 2 columnas del resumen del diagnóstico. */
@Composable
fun SectionScoreGridCell(numero: Int, section: SectionScore, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$numero",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                Text(
                    section.seccionNombre,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CreditBar(label = "Required", score = section.required, color = RequiredNavy)
            CreditBar(label = "Plus", score = section.plus, color = PlusFuchsia)
        }
    }
}
