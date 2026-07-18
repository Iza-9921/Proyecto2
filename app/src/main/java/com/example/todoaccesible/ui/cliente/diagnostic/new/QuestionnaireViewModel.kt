@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.todoaccesible.ui.cliente.diagnostic.new

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.PhotoItem
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestionnaireUiState(
    val loading: Boolean = true,
    val sections: List<SectionEntity> = emptyList(),
    val totalQuestions: Int = 0,
    val currentIndex: Int = 0,
    val currentQuestion: QuestionEntity? = null,
    val currentAnswerValue: AnswerValue? = null,
    val currentComentario: String = "",
    val currentPhotos: List<PhotoItem> = emptyList(),
    val showSubmitConfirm: Boolean = false,
    val showDiscardConfirm: Boolean = false,
    val photoPendingDelete: PhotoItem? = null
) {
    val isFirstQuestion: Boolean get() = currentIndex == 0
    val isLastQuestion: Boolean get() = currentIndex == totalQuestions - 1

    /** No se puede pasar a la siguiente pregunta (ni finalizar) sin responder la actual. */
    val canAdvance: Boolean get() = currentAnswerValue != null
}

class QuestionnaireViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)
    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    private val _sections = MutableStateFlow<List<SectionEntity>>(emptyList())
    private val _showSubmitConfirm = MutableStateFlow(false)
    private val _showDiscardConfirm = MutableStateFlow(false)
    private val _photoPendingDelete = MutableStateFlow<PhotoItem?>(null)

    private val answersByCode: StateFlow<Map<String, AnswerEntity>> = diagnosticRepository
        .observeAnswers(diagnosticId)
        .map { list -> list.associateBy { it.questionCodigo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private data class CurrentQA(val question: QuestionEntity?, val answer: AnswerEntity?)

    private val currentQA: StateFlow<CurrentQA> = combine(_currentIndex, _questions, answersByCode) { index, questions, answers ->
        val question = questions.getOrNull(index)
        CurrentQA(question, question?.let { answers[it.codigo] })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentQA(null, null))

    private val currentPhotos: StateFlow<List<PhotoItem>> = currentQA.flatMapLatest { qa ->
        val answerId = qa.answer?.id
        if (answerId == null) {
            flowOf(emptyList())
        } else {
            diagnosticRepository.observePhotos(answerId).map { photos ->
                photos.map { PhotoItem(it.id, Uri.parse(it.uriPath)) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<QuestionnaireUiState> = combine(
        _sections, _questions, _currentIndex, currentQA, currentPhotos
    ) { sections, questions, index, qa, photos ->
        QuestionnaireUiState(
            loading = questions.isEmpty(),
            sections = sections,
            totalQuestions = questions.size,
            currentIndex = index,
            currentQuestion = qa.question,
            currentAnswerValue = qa.answer?.valor,
            currentComentario = qa.answer?.comentario.orEmpty(),
            currentPhotos = photos,
            showSubmitConfirm = _showSubmitConfirm.value,
            showDiscardConfirm = _showDiscardConfirm.value,
            photoPendingDelete = _photoPendingDelete.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuestionnaireUiState())

    init {
        viewModelScope.launch {
            _sections.value = questionCatalogRepository.getAllSections()
            _questions.value = questionCatalogRepository.getAllQuestions()
        }
    }

    fun selectAnswer(valor: AnswerValue) {
        val question = currentQA.value.question ?: return
        val comentario = currentQA.value.answer?.comentario.orEmpty()
        viewModelScope.launch {
            diagnosticRepository.saveAnswer(diagnosticId, question.codigo, valor, comentario)
        }
    }

    fun onComentarioChange(value: String) {
        val question = currentQA.value.question ?: return
        val valor = currentQA.value.answer?.valor
        viewModelScope.launch {
            diagnosticRepository.saveAnswer(diagnosticId, question.codigo, valor, value)
        }
    }

    fun onPhotoAdded(uri: Uri) {
        val question = currentQA.value.question ?: return
        viewModelScope.launch {
            diagnosticRepository.addPhoto(diagnosticId, question.codigo, uri.toString())
        }
    }

    fun requestDeletePhoto(photo: PhotoItem) {
        _photoPendingDelete.value = photo
    }

    fun confirmDeletePhoto() {
        val photo = _photoPendingDelete.value ?: return
        viewModelScope.launch {
            diagnosticRepository.deletePhoto(photo.id)
            _photoPendingDelete.value = null
        }
    }

    fun dismissDeletePhoto() {
        _photoPendingDelete.value = null
    }

    /** Primera pregunta sin responder (en orden de catálogo), o la última si ya todas tienen respuesta. */
    private fun maxUnlockedIndex(): Int {
        val questions = _questions.value
        val answers = answersByCode.value
        var idx = 0
        while (idx < questions.size - 1 && answers[questions[idx].codigo]?.valor != null) idx++
        return idx
    }

    fun nextQuestion() {
        if (currentQA.value.answer?.valor == null) return
        if (_currentIndex.value < _questions.value.size - 1) _currentIndex.value++
    }

    fun previousQuestion() {
        if (_currentIndex.value > 0) _currentIndex.value--
    }

    /** Salta a la sección, pero nunca más allá de la primera pregunta sin responder (no se puede saltar preguntas). */
    fun jumpToSection(sectionId: String) {
        val index = _questions.value.indexOfFirst { it.seccionId == sectionId }
        if (index >= 0) _currentIndex.value = index.coerceAtMost(maxUnlockedIndex())
    }

    fun requestSubmit() {
        if (currentQA.value.answer?.valor == null) return
        _showSubmitConfirm.value = true
    }
    fun dismissSubmit() { _showSubmitConfirm.value = false }

    fun confirmSubmit(onSubmitted: () -> Unit) {
        viewModelScope.launch {
            diagnosticRepository.submit(diagnosticId)
            _showSubmitConfirm.value = false
            onSubmitted()
        }
    }

    fun requestDiscard() { _showDiscardConfirm.value = true }
    fun dismissDiscard() { _showDiscardConfirm.value = false }

    fun confirmDiscard(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            diagnosticRepository.discardDraft(diagnosticId)
            _showDiscardConfirm.value = false
            onDiscarded()
        }
    }
}
