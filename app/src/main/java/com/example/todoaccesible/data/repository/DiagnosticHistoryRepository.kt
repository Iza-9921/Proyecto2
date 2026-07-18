package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.DiagnosticHistoryEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import kotlinx.coroutines.flow.Flow

/** RF-20: historial completo de revisiones y validaciones de un diagnóstico. */
interface DiagnosticHistoryRepository {
    suspend fun record(
        diagnosticId: Long,
        previousStatus: DiagnosticStatus?,
        newStatus: DiagnosticStatus,
        reviewerId: Long?,
        comentario: String = ""
    )

    fun observeForDiagnostic(diagnosticId: Long): Flow<List<DiagnosticHistoryEntity>>
}
