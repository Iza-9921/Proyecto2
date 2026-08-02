package com.example.todoaccesible.ui.admin.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryQuestionsUiState(
    val categoryName: String = "",
    val allSections: List<SectionEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val loading: Boolean = true
)

/**
 * Preguntas de una sola categoría (pantalla a la que se entra desde "Gestión del cuestionario" ›
 * Categorías › tocar una). Todo en memoria (sin backend/API todavía, ver [QuestionCatalogRepository]):
 * altas, edición completa, borrado, reordenar dentro de la categoría y activar/desactivar por pregunta.
 */
class CategoryQuestionsViewModel(
    private val sectionId: String,
    private val repository: QuestionCatalogRepository
) : ViewModel() {

    val uiState: StateFlow<CategoryQuestionsUiState> = combine(
        repository.observeSections(), repository.observeQuestions()
    ) { sections, questions ->
        CategoryQuestionsUiState(
            categoryName = sections.find { it.id == sectionId }?.nombre ?: "",
            allSections = sections.sortedBy { it.orden },
            questions = questions.filter { it.seccionId == sectionId }.sortedBy { it.orden },
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryQuestionsUiState())

    fun createQuestion(
        targetSeccionId: String,
        concepto: String,
        descripcion: String,
        credito: Credito,
        activa: Boolean,
        imagenReferenciaUri: String?
    ) {
        val trimmed = concepto.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.createQuestion(targetSeccionId, trimmed, descripcion.trim(), credito, activa, imagenReferenciaUri)
        }
    }

    fun updateQuestion(question: QuestionEntity) {
        viewModelScope.launch { repository.updateQuestion(question) }
    }

    fun deleteQuestion(codigo: String) {
        viewModelScope.launch { repository.deleteQuestion(codigo) }
    }

    fun setActive(question: QuestionEntity, activa: Boolean) {
        viewModelScope.launch { repository.updateQuestion(question.copy(activa = activa)) }
    }

    fun moveUp(codigo: String) {
        viewModelScope.launch { repository.moveQuestionUp(codigo) }
    }

    fun moveDown(codigo: String) {
        viewModelScope.launch { repository.moveQuestionDown(codigo) }
    }
}
