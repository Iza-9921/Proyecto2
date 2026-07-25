package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.QuestionReviewStatus

/**
 * Validación del administrador para una pregunta puntual de un diagnóstico
 * (revisión detallada). Existe a lo más un registro por (diagnosticId,
 * questionCodigo); se actualiza (upsert) cada vez que el administrador
 * cambia el estado o el comentario de esa pregunta.
 */
data class QuestionReviewEntity(
    val id: Long = 0,
    val diagnosticId: Long,
    val questionCodigo: String,
    val status: QuestionReviewStatus = QuestionReviewStatus.PENDIENTE,
    val comentario: String = "",
    val reviewerId: Long? = null,
    val fecha: Long = System.currentTimeMillis()
)
