package com.example.todoaccesible.ui.newproject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    viewModel: NewProjectViewModel,
    onNavigateBack: () -> Unit,
    onProjectCreated: (String, String, String, String) -> Unit
) {
    val projectName by viewModel.projectName.collectAsState()
    val address by viewModel.address.collectAsState()
    val responsible by viewModel.contactName.collectAsState()
    val description by viewModel.observations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de Instalación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Detalles del Proyecto",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ingresa la información de la sucursal o sede a evaluar.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = projectName,
                onValueChange = { viewModel.onProjectNameChange(it) },
                label = { Text("Nombre del proyecto o sucursal") },
                placeholder = { Text("Ej. Corporativo Santa Fe") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { viewModel.onAddressChange(it) },
                label = { Text("Dirección completa") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = responsible,
                onValueChange = { viewModel.onContactNameChange(it) },
                label = { Text("Persona Responsable") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.onObservationsChange(it) },
                label = { Text("Descripción / Notas (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { 
                    onProjectCreated(projectName, address, responsible, description)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = projectName.isNotBlank() && address.isNotBlank() && responsible.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("COMENZAR EVALUACIÓN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
