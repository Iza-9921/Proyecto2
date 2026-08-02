package com.example.todoaccesible.ui.admin.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Una fila de la lista de categorías: la sección más el número de preguntas que contiene. */
data class CategoryRow(val section: SectionEntity, val questionCount: Int)

data class CategoryManagementUiState(
    val categories: List<CategoryRow> = emptyList(),
    val loading: Boolean = true
)

/**
 * Módulo "Gestión del cuestionario" (pestaña Categorías): permite crear, renombrar,
 * activar/desactivar, reordenar y eliminar categorías sin tocar código. Desactivar una
 * categoría la excluye del cuestionario nuevo del cliente y del scorecard (ver
 * [QuestionCatalogRepository.getActiveQuestions]), pero conserva el historial ya capturado.
 */
class CategoryManagementViewModel(private val repository: QuestionCatalogRepository) : ViewModel() {

    val uiState: StateFlow<CategoryManagementUiState> = combine(
        repository.observeSections(), repository.observeQuestions()
    ) { sections, questions ->
        val countBySection = questions.groupingBy { it.seccionId }.eachCount()
        CategoryManagementUiState(
            categories = sections.sortedBy { it.orden }.map { CategoryRow(it, countBySection[it.id] ?: 0) },
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryManagementUiState())

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage

    fun createCategory(nombre: String) {
        val trimmed = nombre.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.createSection(trimmed) }
    }

    fun renameCategory(id: String, nombre: String) {
        val trimmed = nombre.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameSection(id, trimmed) }
    }

    fun setActive(id: String, activa: Boolean) {
        viewModelScope.launch { repository.setSectionActive(id, activa) }
    }

    fun moveUp(id: String) {
        viewModelScope.launch { repository.moveSectionUp(id) }
    }

    fun moveDown(id: String) {
        viewModelScope.launch { repository.moveSectionDown(id) }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val deleted = repository.deleteSection(id)
            if (!deleted) {
                _deleteBlockedMessage.value = "No se puede eliminar: esta categoría todavía tiene preguntas asociadas. Elimínalas o muévelas a otra categoría primero."
            }
        }
    }

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }
}
