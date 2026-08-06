package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.QuestionReviewEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import kotlinx.coroutines.flow.map

class QuestionReviewRepositoryImpl(
    private val table: InMemoryTable<QuestionReviewEntity> = InMemoryTable()
) : QuestionReviewRepository {

    override fun observeForDiagnostic(diagnosticId: Long) =
        table.flow.map { list -> list.filter { it.diagnosticId == diagnosticId } }

    override suspend fun setStatus(diagnosticId: Long, questionCodigo: String, status: QuestionReviewStatus, reviewerId: Long) {
        upsert(diagnosticId, questionCodigo) { it.copy(status = status, reviewerId = reviewerId, fecha = System.currentTimeMillis()) }
    }

    override suspend fun setComentario(diagnosticId: Long, questionCodigo: String, comentario: String, reviewerId: Long) {
        upsert(diagnosticId, questionCodigo) { it.copy(comentario = comentario, reviewerId = reviewerId, fecha = System.currentTimeMillis()) }
    }

    override suspend fun resetForResubmission(diagnosticId: Long, questionCodigo: String) {
        upsert(diagnosticId, questionCodigo) {
            it.copy(status = QuestionReviewStatus.PENDIENTE, comentario = "", reviewerId = null, fecha = System.currentTimeMillis())
        }
    }

    private fun upsert(
        diagnosticId: Long,
        questionCodigo: String,
        update: (QuestionReviewEntity) -> QuestionReviewEntity
    ) {
        table.mutate { list ->
            val existing = list.find { it.diagnosticId == diagnosticId && it.questionCodigo == questionCodigo }
            if (existing != null) {
                list.map { if (it === existing) update(it) else it }
            } else {
                list + update(QuestionReviewEntity(id = table.nextId(), diagnosticId = diagnosticId, questionCodigo = questionCodigo))
            }
        }
    }
}
