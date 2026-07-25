package com.example.todoaccesible.ui.admin.review

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.HistoryEntryUi
import com.example.todoaccesible.core.designsystem.PhotoItem
import com.example.todoaccesible.core.util.FileShare
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.QuestionReviewEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.export.excel.ExcelDiagnosticGenerator
import com.example.todoaccesible.export.pdf.PdfScorecardGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewRow(
    val question: QuestionEntity,
    val answer: AnswerEntity?,
    val photos: List<PhotoItem>,
    /** Validación detallada del administrador para esta pregunta (nueva funcionalidad). */
    val reviewStatus: QuestionReviewStatus = QuestionReviewStatus.PENDIENTE,
    val reviewComentario: String = ""
)

private data class ReviewFilters(
    val query: String = "",
    val creditoFilter: Credito? = null,
    val valorFilter: AnswerValue? = null
)

data class AdminReviewUiState(
    val diagnostic: DiagnosticEntity? = null,
    val rows: List<ReviewRow> = emptyList(),
    val query: String = "",
    val creditoFilter: Credito? = null,
    val valorFilter: AnswerValue? = null,
    val comentario: String = "",
    val history: List<HistoryEntryUi> = emptyList()
) {
    val filteredRows: List<ReviewRow> get() = rows.filter { row ->
        (creditoFilter == null || row.question.credito == creditoFilter) &&
            (valorFilter == null || row.answer?.valor == valorFilter) &&
            (query.isBlank() || row.question.concepto.contains(query, ignoreCase = true) || row.question.codigo.contains(query, ignoreCase = true))
    }
}

class AdminReviewViewModel(
    private val diagnosticId: Long,
    private val reviewerId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository,
    private val diagnosticHistoryRepository: DiagnosticHistoryRepository,
    private val userRepository: UserRepository,
    private val questionReviewRepository: QuestionReviewRepository
) : ViewModel() {

    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    private val _sections = MutableStateFlow<List<com.example.todoaccesible.data.local.entities.SectionEntity>>(emptyList())
    private val _photosByAnswer = MutableStateFlow<Map<Long, List<PhotoItem>>>(emptyMap())
    private val _filters = MutableStateFlow(ReviewFilters())
    private val _comentario = MutableStateFlow("")
    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError

    private val answers: StateFlow<List<AnswerEntity>> = diagnosticRepository.observeAnswers(diagnosticId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val questionReviews: StateFlow<List<QuestionReviewEntity>> = questionReviewRepository.observeForDiagnostic(diagnosticId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rowsFlow = combine(_questions, answers, _photosByAnswer, questionReviews) { questions, answerList, photosByAnswer, reviewList ->
        val answersByCode = answerList.associateBy { it.questionCodigo }
        val reviewByCode = reviewList.associateBy { it.questionCodigo }
        questions.map { q ->
            val answer = answersByCode[q.codigo]
            val review = reviewByCode[q.codigo]
            ReviewRow(
                question = q,
                answer = answer,
                photos = answer?.let { photosByAnswer[it.id] } ?: emptyList(),
                reviewStatus = review?.status ?: QuestionReviewStatus.PENDIENTE,
                reviewComentario = review?.comentario ?: ""
            )
        }
    }

    private val historyFlow = combine(
        diagnosticHistoryRepository.observeForDiagnostic(diagnosticId),
        userRepository.observeAll()
    ) { entries, users ->
        val nameById = users.associate { it.id to it.nombre }
        entries.map { entry ->
            HistoryEntryUi(entry, entry.reviewerId?.let { nameById[it] } ?: "Cliente")
        }
    }

    val uiState: StateFlow<AdminReviewUiState> = combine(
        diagnosticRepository.observeById(diagnosticId),
        rowsFlow,
        _filters,
        _comentario,
        historyFlow
    ) { diagnostic, rows, filters, comentario, history ->
        AdminReviewUiState(diagnostic, rows, filters.query, filters.creditoFilter, filters.valorFilter, comentario, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminReviewUiState())

    init {
        viewModelScope.launch {
            _questions.value = questionCatalogRepository.getAllQuestions()
            _sections.value = questionCatalogRepository.getAllSections()
        }
        viewModelScope.launch {
            answers.collect { list ->
                val ids = list.map { it.id }
                val photos = diagnosticRepository.getPhotosForAnswers(ids)
                _photosByAnswer.value = photos.mapValues { (_, v) -> v.map { PhotoItem(it.id, Uri.parse(it.uriPath)) } }
            }
        }
    }

    fun setQuery(value: String) { _filters.value = _filters.value.copy(query = value) }
    fun setCreditoFilter(value: Credito?) { _filters.value = _filters.value.copy(creditoFilter = value) }
    fun setValorFilter(value: AnswerValue?) { _filters.value = _filters.value.copy(valorFilter = value) }
    fun setComentario(value: String) { _comentario.value = value }

    fun setStatus(status: DiagnosticStatus) {
        viewModelScope.launch {
            diagnosticRepository.updateStatus(diagnosticId, status, reviewerId, _comentario.value.trim())
            _comentario.value = ""
        }
    }

    /** Nueva funcionalidad: validación individual del administrador para una pregunta. */
    fun setQuestionReviewStatus(questionCodigo: String, status: QuestionReviewStatus) {
        viewModelScope.launch {
            questionReviewRepository.setStatus(diagnosticId, questionCodigo, status, reviewerId)
        }
    }

    fun setQuestionReviewComentario(questionCodigo: String, comentario: String) {
        viewModelScope.launch {
            questionReviewRepository.setComentario(diagnosticId, questionCodigo, comentario, reviewerId)
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            val diagnostic = uiState.value.diagnostic ?: return@launch
            val unanswered = diagnosticRepository.countUnanswered(diagnosticId)
            if (unanswered > 0) {
                _exportError.value = "Debes contestar todas las preguntas del cuestionario antes de descargar el PDF. Faltan $unanswered."
                return@launch
            }
            val scorecard = diagnosticRepository.recalculateScore(diagnosticId) ?: return@launch
            val file = withContext(Dispatchers.IO) { PdfScorecardGenerator.generate(context, diagnostic, scorecard) }
            FileShare.share(context, file, "application/pdf")
        }
    }

    fun dismissExportError() { _exportError.value = null }

    fun exportExcel(context: Context) {
        viewModelScope.launch {
            val diagnostic = uiState.value.diagnostic ?: return@launch
            val rows = uiState.value.rows
            val file = withContext(Dispatchers.IO) {
                ExcelDiagnosticGenerator.generate(context, diagnostic, rows, _sections.value)
            }
            FileShare.share(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }
    }
}
