package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.DiagnosticHistoryEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import kotlinx.coroutines.flow.map

class DiagnosticHistoryRepositoryImpl(
    private val history: InMemoryTable<DiagnosticHistoryEntity> = InMemoryTable()
) : DiagnosticHistoryRepository {

    override suspend fun record(
        diagnosticId: Long,
        previousStatus: DiagnosticStatus?,
        newStatus: DiagnosticStatus,
        reviewerId: Long?,
        comentario: String
    ) {
        val entry = DiagnosticHistoryEntity(
            id = history.nextId(),
            diagnosticId = diagnosticId,
            previousStatus = previousStatus,
            newStatus = newStatus,
            reviewerId = reviewerId,
            comentario = comentario,
            fecha = System.currentTimeMillis()
        )
        history.mutate { it + entry }
    }

    override fun observeForDiagnostic(diagnosticId: Long) =
        history.flow.map { entries ->
            entries.filter { it.diagnosticId == diagnosticId }.sortedBy { it.fecha }
        }
}
