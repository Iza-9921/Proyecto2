package com.example.todoaccesible.ui.admin.review

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.PhotoItem
import com.example.todoaccesible.core.util.FileShare
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
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
    val photos: List<PhotoItem>
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
    val valorFilter: AnswerValue? = null
) {
    val filteredRows: List<ReviewRow> get() = rows.filter { row ->
        (creditoFilter == null || row.question.credito == creditoFilter) &&
            (valorFilter == null || row.answer?.valor == valorFilter) &&
            (query.isBlank() || row.question.concepto.contains(query, ignoreCase = true) || row.question.codigo.contains(query, ignoreCase = true))
    }
}

class AdminReviewViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository
) : ViewModel() {

    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    private val _sections = MutableStateFlow<List<com.example.todoaccesible.data.local.entities.SectionEntity>>(emptyList())
    private val _photosByAnswer = MutableStateFlow<Map<Long, List<PhotoItem>>>(emptyMap())
    private val _filters = MutableStateFlow(ReviewFilters())

    private val answers: StateFlow<List<AnswerEntity>> = diagnosticRepository.observeAnswers(diagnosticId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rowsFlow = combine(_questions, answers, _photosByAnswer) { questions, answerList, photosByAnswer ->
        val answersByCode = answerList.associateBy { it.questionCodigo }
        questions.map { q ->
            val answer = answersByCode[q.codigo]
            ReviewRow(q, answer, answer?.let { photosByAnswer[it.id] } ?: emptyList())
        }
    }

    val uiState: StateFlow<AdminReviewUiState> = combine(
        diagnosticRepository.observeById(diagnosticId),
        rowsFlow,
        _filters
    ) { diagnostic, rows, filters ->
        AdminReviewUiState(diagnostic, rows, filters.query, filters.creditoFilter, filters.valorFilter)
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

    fun setStatus(status: DiagnosticStatus) {
        viewModelScope.launch { diagnosticRepository.updateStatus(diagnosticId, status) }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            val diagnostic = uiState.value.diagnostic ?: return@launch
            val scorecard = diagnosticRepository.recalculateScore(diagnosticId) ?: return@launch
            val file = withContext(Dispatchers.IO) { PdfScorecardGenerator.generate(context, diagnostic, scorecard) }
            FileShare.share(context, file, "application/pdf")
        }
    }

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
