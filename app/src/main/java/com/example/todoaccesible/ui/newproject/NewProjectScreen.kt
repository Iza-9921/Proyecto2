package com.example.todoaccesible.ui.newproject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    viewModel: NewProjectViewModel,
    onNavigateBack: () -> Unit,
    onStartEvaluation: () -> Unit
) {
    val projectName by viewModel.projectName.collectAsState()
    val address by viewModel.address.collectAsState()
    val city by viewModel.city.collectAsState()
    val propertyType by viewModel.propertyType.collectAsState()
    val contactName by viewModel.contactName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos del Inmueble") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
            Text(
                text = "Nuevo Proyecto",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = projectName,
                onValueChange = { viewModel.onProjectNameChange(it) },
                label = { Text("Nombre del Proyecto/Inmueble") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { viewModel.onAddressChange(it) },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { viewModel.onCityChange(it) },
                label = { Text("Ciudad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = propertyType,
                onValueChange = { viewModel.onPropertyTypeChange(it) },
                label = { Text("Tipo de Inmueble (Oficina, Local, etc.)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contactName,
                onValueChange = { viewModel.onContactNameChange(it) },
                label = { Text("Nombre del Contacto en Sitio") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.startEvaluation(onStartEvaluation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = projectName.isNotBlank() && address.isNotBlank() && city.isNotBlank()
            ) {
                Text("COMENZAR EVALUACIÓN")
            }
        }
    }
}
