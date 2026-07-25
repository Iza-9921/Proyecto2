package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.QuestionReviewEntity
import com.example.todoaccesible.data.model.QuestionReviewStatus
import kotlinx.coroutines.flow.Flow

/** Validación detallada por pregunta que hace el administrador al revisar un diagnóstico. */
interface QuestionReviewRepository {
    fun observeForDiagnostic(diagnosticId: Long): Flow<List<QuestionReviewEntity>>

    suspend fun setStatus(diagnosticId: Long, questionCodigo: String, status: QuestionReviewStatus, reviewerId: Long)

    suspend fun setComentario(diagnosticId: Long, questionCodigo: String, comentario: String, reviewerId: Long)
}
