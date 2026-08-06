package com.example.todoaccesible.ui.cliente.diagnostic.new

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.designsystem.BigTouchButton
import com.example.todoaccesible.core.designsystem.BigTouchOutlinedButton
import com.example.todoaccesible.core.designsystem.EmpresaInfoFields

@Composable
fun ProjectInfoScreen(
    viewModel: ProjectInfoViewModel,
    onNavigateBack: () -> Unit,
    onContinue: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de la empresa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Estos datos aparecen en la portada del scorecard.",
                style = MaterialTheme.typography.bodyMedium
            )
            EmpresaInfoFields(
                projectName = uiState.projectName,
                onProjectNameChange = viewModel::onProjectNameChange,
                clienteNombre = uiState.clienteNombre,
                onClienteNombreChange = viewModel::onClienteNombreChange,
                telefono = uiState.telefono,
                onTelefonoChange = viewModel::onTelefonoChange,
                ubicacion = uiState.ubicacion,
                onUbicacionChange = viewModel::onUbicacionChange,
                entidadFederativa = uiState.entidadFederativa,
                onEntidadFederativaChange = viewModel::onEntidadFederativaChange,
                ciudad = uiState.ciudad,
                onCiudadChange = viewModel::onCiudadChange,
                tipoInmueble = uiState.tipoInmueble,
                onTipoInmuebleChange = {},
                tipoInmuebleReadOnly = true,
                fechaEvaluacion = uiState.fechaEvaluacion,
                onFechaEvaluacionChange = viewModel::onFechaEvaluacionChange,
                responsable = uiState.responsable,
                onResponsableChange = viewModel::onResponsableChange,
                revision = uiState.revision,
                onRevisionChange = viewModel::onRevisionChange,
                logoEmpresaUri = uiState.logoEmpresaUri,
                onLogoEmpresaChange = viewModel::onLogoEmpresaChange,
                showRevision = false
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigTouchOutlinedButton(
                    text = "Atrás",
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                )
                BigTouchButton(
                    text = "Comenzar cuestionario",
                    enabled = !uiState.loading && uiState.projectName.isNotBlank(),
                    onClick = { viewModel.continueToQuestionnaire(onContinue) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (uiState.showResumeDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResumeDialog,
            title = { Text("Continuar donde me quedé") },
            text = { Text("Ya tenías avance en un diagnóstico sin enviar. ¿Quieres continuarlo o empezar de nuevo?") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissResumeDialog) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::startOver) { Text("Empezar de nuevo") }
            }
        )
    }
}
