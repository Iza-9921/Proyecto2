package com.example.todoaccesible.ui.admin.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestionCatalogUiState(
    val sections: List<SectionEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val editing: QuestionEntity? = null
)

class QuestionCatalogViewModel(private val repository: QuestionCatalogRepository) : ViewModel() {

    private val _editing = MutableStateFlow<QuestionEntity?>(null)

    val uiState: StateFlow<QuestionCatalogUiState> = combine(
        repository.observeSections(), repository.observeQuestions(), _editing
    ) { sections, questions, editing ->
        QuestionCatalogUiState(sections, questions, editing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuestionCatalogUiState())

    fun startEditing(question: QuestionEntity) { _editing.value = question }
    fun cancelEditing() { _editing.value = null }

    fun saveEditing(concepto: String, admiteFoto: Boolean) {
        val question = _editing.value ?: return
        viewModelScope.launch {
            repository.updateQuestion(question.copy(concepto = concepto, admiteFoto = admiteFoto))
            _editing.value = null
        }
    }
}
