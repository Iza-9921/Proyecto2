package com.example.todoaccesible.ui.cliente.diagnostic.result

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.PhotoItem
import com.example.todoaccesible.core.util.FileShare
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.domain.scoring.ScorecardResult
import com.example.todoaccesible.export.excel.ExcelDiagnosticGenerator
import com.example.todoaccesible.export.pdf.PdfScorecardGenerator
import com.example.todoaccesible.ui.admin.review.ReviewRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticResultUiState(
    val loading: Boolean = true,
    val diagnostic: DiagnosticEntity? = null,
    val scorecard: ScorecardResult? = null,
    val exporting: Boolean = false,
    val exportError: String? = null
)

/**
 * Pantalla mostrada inmediatamente después de "Finalizar diagnóstico" (RF:
 * flujo post-envío). El diagnóstico ya fue guardado y enviado a revisión por
 * `QuestionnaireViewModel.confirmSubmit` antes de llegar aquí; esta pantalla
 * solo muestra el resultado preliminar y permite descargar PDF/Excel.
 */
class DiagnosticResultViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticResultUiState())
    val uiState: StateFlow<DiagnosticResultUiState> = _uiState

    init {
        viewModelScope.launch {
            val diagnostic = diagnosticRepository.getById(diagnosticId)
            val scorecard = diagnosticRepository.recalculateScore(diagnosticId)
            _uiState.value = _uiState.value.copy(loading = false, diagnostic = diagnostic, scorecard = scorecard)
        }
    }

    fun exportPdf(context: Context) {
        val diagnostic = _uiState.value.diagnostic ?: return
        val scorecard = _uiState.value.scorecard ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true)
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfScorecardGenerator.generate(context, diagnostic, scorecard, esDefinitivo = false)
                }
                FileShare.share(context, file, "application/pdf")
            } finally {
                _uiState.value = _uiState.value.copy(exporting = false)
            }
        }
    }

    fun exportExcel(context: Context) {
        val diagnostic = _uiState.value.diagnostic ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true)
            try {
                val questions = questionCatalogRepository.getAllQuestions()
                val sections = questionCatalogRepository.getAllSections()
                val answers = diagnosticRepository.observeAnswers(diagnosticId).first().associateBy { it.questionCodigo }
                val photosByAnswer = diagnosticRepository.getPhotosForAnswers(answers.values.map { it.id })
                val rows = questions.map { question ->
                    val answer = answers[question.codigo]
                    ReviewRow(
                        question = question,
                        answer = answer,
                        photos = answer?.let { photosByAnswer[it.id] }.orEmpty().map { PhotoItem(it.id, Uri.parse(it.uriPath)) },
                        reviewStatus = QuestionReviewStatus.PENDIENTE,
                        reviewComentario = ""
                    )
                }
                val file = withContext(Dispatchers.IO) {
                    ExcelDiagnosticGenerator.generate(context, diagnostic, rows, sections)
                }
                FileShare.share(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            } finally {
                _uiState.value = _uiState.value.copy(exporting = false)
            }
        }
    }

    fun dismissExportError() {
        _uiState.value = _uiState.value.copy(exportError = null)
    }
}
