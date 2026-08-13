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
import com.example.todoaccesible.data.repository.PresenceRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.domain.scoring.ScorecardCalculator
import com.example.todoaccesible.domain.scoring.ScorecardQuestion
import com.example.todoaccesible.domain.scoring.ScorecardResult
import com.example.todoaccesible.domain.scoring.toAnswerValue
import com.example.todoaccesible.export.excel.ExcelDiagnosticGenerator
import com.example.todoaccesible.export.pdf.PdfScorecardGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
    val history: List<HistoryEntryUi> = emptyList(),
    /** Cuantificación en vivo con la calificación que el admin lleva capturada hasta ahora (se recalcula con cada cambio). */
    val liveScorecard: ScorecardResult? = null
) {
    val filteredRows: List<ReviewRow> get() = rows.filter { row ->
        (creditoFilter == null || row.question.credito == creditoFilter) &&
            (valorFilter == null || row.answer?.valor == valorFilter) &&
            (query.isBlank() || row.question.concepto.contains(query, ignoreCase = true) || row.question.codigo.contains(query, ignoreCase = true))
    }

    /** Preguntas cuya validación del admin sigue abierta (pendiente o esperando info del cliente). */
    val pendingReviewCount: Int get() = rows.count {
        it.reviewStatus == QuestionReviewStatus.PENDIENTE || it.reviewStatus == QuestionReviewStatus.SOLICITAR_INFO
    }

    /** Solo se puede finalizar (Validar) cuando ya no queda ninguna pregunta sin dictamen del admin. */
    val canFinalize: Boolean get() = rows.isNotEmpty() && pendingReviewCount == 0
}

class AdminReviewViewModel(
    private val diagnosticId: Long,
    private val reviewerId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository,
    private val diagnosticHistoryRepository: DiagnosticHistoryRepository,
    private val userRepository: UserRepository,
    private val questionReviewRepository: QuestionReviewRepository,
    private val presenceRepository: PresenceRepository
) : ViewModel() {

    /** RF: "X está revisando este diagnóstico" — otros admins con latido reciente en esta pantalla. */
    val otherViewers: StateFlow<List<String>> = presenceRepository.observeViewers(diagnosticId, reviewerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /** Recalcula el scorecard con la misma fórmula del PDF/oficial, pero en vivo con cada cambio de calificación. */
    private val liveScorecardFlow = combine(rowsFlow, _sections) { rows, sections ->
        if (rows.isEmpty()) return@combine null
        val sectionNameById = sections.associate { it.id to it.nombre }
        val scorecardQuestions = rows.map { row ->
            ScorecardQuestion(
                codigo = row.question.codigo,
                seccionId = row.question.seccionId,
                seccionNombre = sectionNameById[row.question.seccionId] ?: row.question.seccionId,
                credito = row.question.credito
            )
        }
        val answers = rows.associate { it.question.codigo to it.reviewStatus.toAnswerValue() }
        ScorecardCalculator.calculate(scorecardQuestions, answers)
    }

    private val baseUiState = combine(
        diagnosticRepository.observeById(diagnosticId),
        rowsFlow,
        _filters,
        _comentario,
        historyFlow
    ) { diagnostic, rows, filters, comentario, history ->
        AdminReviewUiState(diagnostic, rows, filters.query, filters.creditoFilter, filters.valorFilter, comentario, history)
    }

    val uiState: StateFlow<AdminReviewUiState> = combine(baseUiState, liveScorecardFlow) { state, liveScorecard ->
        state.copy(liveScorecard = liveScorecard)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminReviewUiState())

    init {
        viewModelScope.launch {
            val tipo = diagnosticRepository.getById(diagnosticId)?.tipoInmueble?.ifBlank { "Otro" } ?: "Otro"
            _questions.value = questionCatalogRepository.getAllQuestions(tipo)
            _sections.value = questionCatalogRepository.getAllSections(tipo)
        }
        viewModelScope.launch {
            answers.collect { list ->
                val ids = list.map { it.id }
                val photos = diagnosticRepository.getPhotosForAnswers(ids)
                _photosByAnswer.value = photos.mapValues { (_, v) -> v.map { PhotoItem(it.id, Uri.parse(it.uriPath)) } }
            }
        }
        viewModelScope.launch {
            val reviewerName = userRepository.observeById(reviewerId).firstOrNull()?.nombre ?: "Un administrador"
            while (isActive) {
                presenceRepository.heartbeat(diagnosticId, reviewerId, reviewerName)
                kotlinx.coroutines.delay(30_000L)
            }
        }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    override fun onCleared() {
        super.onCleared()
        // viewModelScope ya está cancelado en este punto; se usa GlobalScope a propósito
        // para que el `clear` (borrar el latido de presencia) sí llegue a ejecutarse.
        kotlinx.coroutines.GlobalScope.launch { presenceRepository.clear(diagnosticId, reviewerId) }
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

    /**
     * Finaliza la evaluación: recalcula el resultado oficial con la
     * validación por pregunta ya hecha por el admin y pasa el diagnóstico a
     * VALIDADO. Distinto de `setStatus(VALIDADO)` porque además recalcula el
     * scorecard (no solo cambia el estado).
     */
    fun finalizeEvaluation() {
        viewModelScope.launch {
            diagnosticRepository.finalizeOfficialScore(diagnosticId, reviewerId, _comentario.value.trim())
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

    /**
     * PDF "del administrador": se calcula desde la validación por pregunta
     * que ya lleva capturada el admin (`QuestionReviewEntity`), disponible
     * en cualquier momento de la revisión (no solo tras validar) — las
     * preguntas aún sin dictamen puntúan como pendientes, igual que en la
     * web ("PDF del administrador" al lado de "PDF del cliente" en
     * `RevisarDiagnostico.jsx`, ambos disponibles desde el inicio de la revisión).
     */
    fun exportPdfDefinitivo(context: Context) {
        viewModelScope.launch {
            val diagnostic = uiState.value.diagnostic ?: return@launch
            val esDefinitivo = diagnostic.estado == DiagnosticStatus.VALIDADO
            val scorecard = withContext(Dispatchers.Default) { diagnosticRepository.getOfficialScore(diagnosticId) } ?: return@launch
            val file = withContext(Dispatchers.IO) {
                PdfScorecardGenerator.generate(context, diagnostic, scorecard, esDefinitivo = esDefinitivo)
            }
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
