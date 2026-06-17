package com.example.todoaccesible.ui.evaluation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.todoaccesible.data.model.AnswerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    projectId: String,
    viewModel: EvaluationViewModel,
    onNavigateBack: () -> Unit,
    onTakePhoto: (Int) -> Unit,
    onEvaluationFinished: (String) -> Unit
) {
    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val currentQuestion = questions[currentIndex]
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onPhotoCaptured(currentIndex, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encuesta de Accesibilidad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
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
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = currentQuestion.category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Pregunta ${currentIndex + 1} de ${questions.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentQuestion.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de respuestas (4 opciones)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnswerChip(
                        text = "SÍ",
                        selected = currentQuestion.answer == AnswerType.SI,
                        onClick = { viewModel.onAnswerChange(currentIndex, AnswerType.SI) },
                        modifier = Modifier.weight(1f)
                    )
                    AnswerChip(
                        text = "PARCIAL",
                        selected = currentQuestion.answer == AnswerType.PARCIALMENTE,
                        onClick = { viewModel.onAnswerChange(currentIndex, AnswerType.PARCIALMENTE) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnswerChip(
                        text = "NO",
                        selected = currentQuestion.answer == AnswerType.NO,
                        onClick = { viewModel.onAnswerChange(currentIndex, AnswerType.NO) },
                        modifier = Modifier.weight(1f)
                    )
                    AnswerChip(
                        text = "N/A",
                        selected = currentQuestion.answer == AnswerType.NA,
                        onClick = { viewModel.onAnswerChange(currentIndex, AnswerType.NA) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Evidencia Fotográfica", 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (currentQuestion.photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(currentQuestion.photoUri),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Adjunta una foto para validar", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { onTakePhoto(currentIndex) },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("CÁMARA", fontSize = 12.sp)
                }
                
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("GALERÍA", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { viewModel.previousQuestion() },
                    enabled = currentIndex > 0
                ) { Text("ANTERIOR") }
                
                Button(
                    onClick = { 
                        if (currentIndex < questions.size - 1) {
                            viewModel.nextQuestion()
                        } else {
                            // Simulamos guardado o llamamos a la función de guardado
                            onEvaluationFinished(projectId)
                        }
                    },
                    enabled = currentQuestion.answer != null
                ) { 
                    Text(if (currentIndex < questions.size - 1) "SIGUIENTE" else "FINALIZAR") 
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        modifier = modifier
    )
}
