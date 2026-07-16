package com.example.todoaccesible.ui.admin.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.domain.scoring.ScorecardResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompareUiState(
    val diagnosticA: DiagnosticEntity? = null,
    val scorecardA: ScorecardResult? = null,
    val diagnosticB: DiagnosticEntity? = null,
    val scorecardB: ScorecardResult? = null
)

class CompareDiagnosticsViewModel(
    private val diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    val allDiagnostics: StateFlow<List<DiagnosticEntity>> = diagnosticRepository.observeAllSubmitted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState

    fun selectA(diagnostic: DiagnosticEntity) {
        viewModelScope.launch {
            val score = diagnosticRepository.recalculateScore(diagnostic.id)
            _uiState.value = _uiState.value.copy(diagnosticA = diagnostic, scorecardA = score)
        }
    }

    fun selectB(diagnostic: DiagnosticEntity) {
        viewModelScope.launch {
            val score = diagnosticRepository.recalculateScore(diagnostic.id)
            _uiState.value = _uiState.value.copy(diagnosticB = diagnostic, scorecardB = score)
        }
    }
}
