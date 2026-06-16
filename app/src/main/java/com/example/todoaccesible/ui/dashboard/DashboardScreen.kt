package com.example.todoaccesible.ui.dashboard

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.data.local.entities.ProjectEntity
import com.example.todoaccesible.data.model.UserRole
import com.example.todoaccesible.ui.specialist.activity.RevisarDiagnosticoActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNewProject: () -> Unit,
    onNavigateToProjectDetail: (String) -> Unit
) {
    val userRole by viewModel.userRole.collectAsState()
    val projects by viewModel.projectsFlow.collectAsState(initial = emptyList())
    val pendingDiagnostics by viewModel.pendingDiagnostics.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (userRole == UserRole.CLIENTE) "Todo Accesible - Mis Proyectos" else "Panel de Especialista", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            // Solo el cliente puede crear nuevos proyectos
            if (userRole == UserRole.CLIENTE) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToNewProject,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("NUEVO PROYECTO") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (userRole == UserRole.CLIENTE) {
                if (projects.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes proyectos registrados", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(projects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onNavigateToProjectDetail(project.id) }
                            )
                        }
                    }
                }
            } else {
                // Vista para el Especialista: Solo ve diagnósticos pendientes
                Text(
                    text = "Diagnósticos Pendientes de Revisión",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pendingDiagnostics) { diagnostic ->
                        DiagnosticCard(
                            diagnostic = diagnostic,
                            onReviewClick = {
                                val intent = Intent(context, RevisarDiagnosticoActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(project: ProjectEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                ComplianceBadge(percentage = project.compliancePercentage)
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = "Estado: ${project.status}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Fecha: ${project.lastEvaluationDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { project.compliancePercentage / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun ComplianceBadge(percentage: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$percentage%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiagnosticCard(diagnostic: Diagnostic, onReviewClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(diagnostic.projectName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Cliente: ${diagnostic.clientName}", style = MaterialTheme.typography.bodyMedium)
            Text("Fecha: ${diagnostic.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReviewClick) { Text("VER DETALLES") }
                Button(onClick = onReviewClick) { Text("REVISAR") }
            }
        }
    }
}
