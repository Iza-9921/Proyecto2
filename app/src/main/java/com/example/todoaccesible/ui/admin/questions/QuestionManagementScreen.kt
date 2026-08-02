package com.example.todoaccesible.ui.admin.questions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Módulo "Gestión del cuestionario": el administrador crea y administra las categorías y las
 * preguntas del diagnóstico sin tocar código. Por ahora la pestaña "Categorías" tiene el CRUD
 * completo (crear, renombrar, activar/desactivar, ordenar, eliminar); "Preguntas" reutiliza el
 * catálogo existente (edición de concepto/evidencia fotográfica).
 */
@Composable
fun QuestionManagementScreen(
    categoryViewModel: CategoryManagementViewModel,
    questionViewModel: QuestionCatalogViewModel,
    onOpenCategory: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Categorías", "Preguntas")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gestión del cuestionario") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }
            when (selectedTab) {
                0 -> CategoryManagementScreen(viewModel = categoryViewModel, onOpenCategory = onOpenCategory, modifier = Modifier.fillMaxSize())
                else -> QuestionCatalogContent(viewModel = questionViewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
