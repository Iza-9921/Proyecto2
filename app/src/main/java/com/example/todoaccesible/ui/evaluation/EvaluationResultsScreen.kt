package com.example.todoaccesible.ui.evaluation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoaccesible.data.model.EvaluationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationResultsScreen(
    result: EvaluationResult,
    onContinue: () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = result.percentage / 100f,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    val statusColor = when {
        result.percentage >= 90 -> Color(0xFF4CAF50) // Verde
        result.percentage >= 75 -> Color(0xFF8BC34A) // Verde claro
        result.percentage >= 60 -> Color(0xFFFFC107) // Amarillo
        result.percentage >= 40 -> Color(0xFFFF9800) // Naranja
        else -> Color(0xFFF44336) // Rojo
    }

    val dynamicMessage = when {
        result.percentage >= 90 -> "Tu empresa cuenta con un alto nivel de accesibilidad. Continúa manteniendo y fortaleciendo estas buenas prácticas."
        result.percentage >= 75 -> "Tu empresa presenta buenas condiciones de accesibilidad, aunque existen áreas que pueden mejorar."
        result.percentage >= 60 -> "Se identificaron oportunidades importantes de mejora para brindar una experiencia más inclusiva."
        result.percentage >= 40 -> "Existen diversas barreras de accesibilidad que deberían atenderse prioritariamente."
        else -> "Se recomienda una intervención integral para mejorar la accesibilidad de las instalaciones."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultados de Accesibilidad", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Indicador Visual Circular
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.fillMaxSize(),
                    color = statusColor,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${result.percentage}%",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                    Text(
                        text = "Accesibilidad",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nivel de Accesibilidad
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
            ) {
                Text(
                    text = result.level.uppercase(),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mensaje Dinámico
            Text(
                text = dynamicMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Resumen de Respuestas
            Text(
                text = "Resumen de respuestas",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ResultRow("Total de Sí", result.countSi.toString(), Color(0xFF4CAF50))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    ResultRow("Total de Parcialmente", result.countParcialmente.toString(), Color(0xFFFFC107))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    ResultRow("Total de No", result.countNo.toString(), Color(0xFFF44336))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    ResultRow("Total de No Aplica", result.countNA.toString(), Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("CONTINUAR", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(12.dp), color = color, shape = androidx.compose.foundation.shape.CircleShape) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}
