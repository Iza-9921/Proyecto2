package com.example.todoaccesible.ui.cliente.diagnostic.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.util.FileShare
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.domain.scoring.ScorecardResult
import com.example.todoaccesible.export.pdf.PdfScorecardGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticDetailUiState(
    val loading: Boolean = true,
    val diagnostic: DiagnosticEntity? = null,
    val scorecard: ScorecardResult? = null
)

class DiagnosticDetailViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository
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
    }

    fun exportPdf(context: Context) {
        val diagnostic = _uiState.value.diagnostic ?: return
        val scorecard = _uiState.value.scorecard ?: return
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfScorecardGenerator.generate(context, diagnostic, scorecard)
            }
            FileShare.share(context, file, "application/pdf")
        }
    }
}
