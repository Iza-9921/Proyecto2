package com.example.todoaccesible.ui.cliente.diagnostic.new

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton

@Composable
fun ProjectInfoScreen(
    viewModel: ProjectInfoViewModel,
    onNavigateBack: () -> Unit,
    onContinue: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Datos del proyecto") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Estos datos aparecen en la portada del scorecard.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = uiState.projectName,
                onValueChange = viewModel::onProjectNameChange,
                label = { Text("Nombre del proyecto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.ubicacion,
                onValueChange = viewModel::onUbicacionChange,
                label = { Text("Ubicación") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.responsable,
                onValueChange = viewModel::onResponsableChange,
                label = { Text("Responsable") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.revision,
                onValueChange = viewModel::onRevisionChange,
                label = { Text("Revisión") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            BigTouchButton(
                text = "Comenzar cuestionario",
                enabled = !uiState.loading && uiState.projectName.isNotBlank(),
                onClick = { viewModel.continueToQuestionnaire(onContinue) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
