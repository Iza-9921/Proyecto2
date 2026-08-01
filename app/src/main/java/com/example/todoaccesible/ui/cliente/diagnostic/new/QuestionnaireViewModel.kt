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
    val showDiscardConfirm: Boolean = false,
    val photoPendingDelete: PhotoItem? = null
) {
    val isFirstQuestion: Boolean get() = currentIndex == 0
    val isLastQuestion: Boolean get() = currentIndex == totalQuestions - 1

    /** No se puede pasar a la siguiente pregunta (ni finalizar) sin responder la actual. */
    val canAdvance: Boolean get() = currentAnswerValue != null
}

/** Fila del listado de estado de preguntas de una categoría (panel "Ver preguntas"). */
data class QuestionStatusItem(
    val index: Int,
    val numero: Int,
    val question: QuestionEntity,
    val answered: Boolean
)

class QuestionnaireViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)
    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    private val _sections = MutableStateFlow<List<SectionEntity>>(emptyList())

    /** Última pregunta visitada por cada sección, para volver exactamente ahí al cambiar de categoría. */
    private val lastIndexBySection = mutableMapOf<String, Int>()
    private val _showDiscardConfirm = MutableStateFlow(false)
    private val _photoPendingDelete = MutableStateFlow<PhotoItem?>(null)

    private val answersByCode: StateFlow<Map<String, AnswerEntity>> = diagnosticRepository
        .observeAnswers(diagnosticId)
        .map { list -> list.associateBy { it.questionCodigo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Por sección: `true` si ya todas sus preguntas tienen respuesta (🟢), `false` si falta alguna (🟡). */
    val sectionCompletion: StateFlow<Map<String, Boolean>> = combine(_questions, answersByCode) { questions, answers ->
        questions.groupBy { it.seccionId }.mapValues { (_, qs) -> qs.all { answers[it.codigo]?.valor != null } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _submitBlockedMessage = MutableStateFlow<String?>(null)
    val submitBlockedMessage: StateFlow<String?> = _submitBlockedMessage

    private val _showQuestionList = MutableStateFlow(false)
    val showQuestionList: StateFlow<Boolean> = _showQuestionList

    private data class CurrentQA(val question: QuestionEntity?, val answer: AnswerEntity?)

    private val currentQA: StateFlow<CurrentQA> = combine(_currentIndex, _questions, answersByCode) { index, questions, answers ->
        val question = questions.getOrNull(index)
        CurrentQA(question, question?.let { answers[it.codigo] })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentQA(null, null))

    /** Listado (número, texto y estado) de todas las preguntas de la categoría actual, para el panel "Ver preguntas". */
    val currentSectionQuestions: StateFlow<List<QuestionStatusItem>> = combine(_questions, currentQA, answersByCode) { questions, qa, answers ->
        val sectionId = qa.question?.seccionId ?: return@combine emptyList()
        questions.withIndex()
            .filter { it.value.seccionId == sectionId }
            .map { (index, question) ->
                QuestionStatusItem(
                    index = index,
                    numero = index + 1,
                    question = question,
                    answered = answers[question.codigo]?.valor != null
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            showDiscardConfirm = _showDiscardConfirm.value,
            photoPendingDelete = _photoPendingDelete.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuestionnaireUiState())

    init {
        viewModelScope.launch {
            _sections.value = questionCatalogRepository.getAllSections()
            _questions.value = questionCatalogRepository.getAllQuestions()
            moveTo(_currentIndex.value)
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

    /** Mueve el índice actual y recuerda esa posición como el último punto visitado de su sección. */
    private fun moveTo(index: Int) {
        val question = _questions.value.getOrNull(index) ?: return
        lastIndexBySection[question.seccionId] = index
        _currentIndex.value = index
    }

    /** Posición actual dentro de su propia categoría: la guardada en `lastIndexBySection`, nunca la de otra sección. */
    private fun currentIndexForOwnSection(): Int {
        val question = currentQA.value.question ?: return _currentIndex.value
        return lastIndexBySection[question.seccionId] ?: _currentIndex.value
    }

    /**
     * Busca, después de [sectionId] (en el orden de las secciones), la primera que aún tenga
     * alguna pregunta sin responder. Devuelve `null` si todas las secciones posteriores ya
     * están completas.
     */
    private fun nextPendingSectionAfter(sectionId: String): String? {
        val order = _sections.value.map { it.id }
        val currentPos = order.indexOf(sectionId)
        if (currentPos == -1) return null
        val answers = answersByCode.value
        val questionsBySection = _questions.value.groupBy { it.seccionId }
        for (i in currentPos + 1 until order.size) {
            val sectionQuestions = questionsBySection[order[i]] ?: continue
            if (sectionQuestions.any { answers[it.codigo]?.valor == null }) return order[i]
        }
        return null
    }

    fun nextQuestion() {
        if (currentQA.value.answer?.valor == null) return
        val current = currentIndexForOwnSection()
        val currentSectionId = _questions.value.getOrNull(current)?.seccionId
        val isLastInSection = currentSectionId != null &&
            _questions.value.withIndex().filter { it.value.seccionId == currentSectionId }
                .maxOf { it.index } == current
        if (isLastInSection) {
            val nextSectionId = nextPendingSectionAfter(currentSectionId)
            if (nextSectionId != null) {
                jumpToSection(nextSectionId)
                return
            }
        }
        if (current < _questions.value.size - 1) moveTo(current + 1)
    }

    fun previousQuestion() {
        val current = currentIndexForOwnSection()
        if (current > 0) moveTo(current - 1)
    }

    /**
     * Navegación libre entre categorías: vuelve exactamente a la última pregunta que el
     * usuario visitó en esa sección (para que "Siguiente" continúe desde ahí, sin reiniciar
     * el recorrido ni mezclar el índice con el de otra categoría). Si nunca la ha visitado,
     * salta a la última ya respondida (para continuar donde se quedó) o a la primera si aún
     * no tiene ninguna respuesta. Nunca pierde las respuestas ni fotos ya capturadas.
     */
    fun jumpToSection(sectionId: String) {
        val sectionQuestions = _questions.value.withIndex().filter { it.value.seccionId == sectionId }
        if (sectionQuestions.isEmpty()) return
        val remembered = lastIndexBySection[sectionId]
        if (remembered != null) {
            moveTo(remembered)
            return
        }
        val answers = answersByCode.value
        val lastAnsweredIndex = sectionQuestions.lastOrNull { answers[it.value.codigo]?.valor != null }?.index
        moveTo(lastAnsweredIndex ?: sectionQuestions.first().index)
    }

    fun openQuestionList() { _showQuestionList.value = true }
    fun dismissQuestionList() { _showQuestionList.value = false }

    /** Navegación rápida desde el panel "Ver preguntas": va directo a la pregunta elegida sin perder respuestas ni fotos. */
    fun goToQuestion(index: Int) {
        moveTo(index)
        dismissQuestionList()
    }

    /**
     * Al terminar la última pregunta: si aún faltan respuestas en cualquier
     * categoría, avisa y no navega. Si ya están todas contestadas, va directo
     * al resumen (sin enviar el diagnóstico todavía; eso lo hace el usuario
     * explícitamente con "Enviar diagnóstico" desde esa pantalla).
     */
    fun goToSummary(onSummary: () -> Unit) {
        if (currentQA.value.answer?.valor == null) return
        viewModelScope.launch {
            val unanswered = diagnosticRepository.countUnanswered(diagnosticId)
            if (unanswered > 0) {
                _submitBlockedMessage.value = "Debes responder todas las preguntas antes de ir al resumen. Aún tienes preguntas pendientes en una o más categorías."
            } else {
                onSummary()
            }
        }
    }
    fun dismissSubmitBlocked() { _submitBlockedMessage.value = null }

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
