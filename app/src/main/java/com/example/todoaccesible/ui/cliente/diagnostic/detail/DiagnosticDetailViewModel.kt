package com.example.todoaccesible.ui.cliente.diagnostic.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.HistoryEntryUi
import com.example.todoaccesible.core.util.FileShare
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.domain.scoring.ScorecardResult
import com.example.todoaccesible.export.pdf.PdfScorecardGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticDetailUiState(
    val loading: Boolean = true,
    val diagnostic: DiagnosticEntity? = null,
    val scorecard: ScorecardResult? = null,
    val history: List<HistoryEntryUi> = emptyList(),
    /** RF-10: no se puede descargar el PDF si faltan preguntas por contestar. */
    val exportError: String? = null
)

class DiagnosticDetailViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val diagnosticHistoryRepository: DiagnosticHistoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticDetailUiState())
    val uiState: StateFlow<DiagnosticDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            diagnosticRepository.observeById(diagnosticId).collect { diagnostic ->
                _uiState.value = _uiState.value.copy(diagnostic = diagnostic)
            }
        }
        viewModelScope.launch {
            val scorecard = diagnosticRepository.recalculateScore(diagnosticId)
            _uiState.value = _uiState.value.copy(scorecard = scorecard, loading = false)
        }
        viewModelScope.launch {
            combine(
                diagnosticHistoryRepository.observeForDiagnostic(diagnosticId),
                userRepository.observeAll()
            ) { entries, users ->
                val nameById = users.associate { it.id to it.nombre }
                entries.map { entry -> HistoryEntryUi(entry, entry.reviewerId?.let { nameById[it] } ?: "Cliente") }
            }.collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }

    fun exportPdf(context: Context) {
        val diagnostic = _uiState.value.diagnostic ?: return
        val scorecard = _uiState.value.scorecard ?: return
        viewModelScope.launch {
            val unanswered = diagnosticRepository.countUnanswered(diagnosticId)
            if (unanswered > 0) {
                _uiState.value = _uiState.value.copy(
                    exportError = "Debes contestar todas las preguntas del cuestionario antes de descargar el PDF. Faltan $unanswered."
                )
                return@launch
            }
            // Ya validado por el admin: el PDF definitivo usa el resultado oficial, no el preliminar del cliente.
            val esDefinitivo = diagnostic.estado == DiagnosticStatus.VALIDADO
            val scorecardParaPdf = if (esDefinitivo) {
                diagnosticRepository.getOfficialScore(diagnosticId) ?: scorecard
            } else {
                scorecard
            }
            val file = withContext(Dispatchers.IO) {
                PdfScorecardGenerator.generate(context, diagnostic, scorecardParaPdf, esDefinitivo)
            }
            FileShare.share(context, file, "application/pdf")
        }
    }

    fun dismissExportError() {
        _uiState.value = _uiState.value.copy(exportError = null)
    }
}
