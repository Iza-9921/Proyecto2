package com.example.todoaccesible.domain.scoring

import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.QuestionReviewStatus

/**
 * Traduce la validación del administrador (por pregunta) al mismo tipo que
 * usa [ScorecardCalculator], para poder calcular el resultado OFICIAL con la
 * misma fórmula que el preliminar. `SOLICITAR_INFO` se trata como pendiente:
 * aún no hay un veredicto final para esa pregunta.
 */
fun QuestionReviewStatus.toAnswerValue(): AnswerValue = when (this) {
    QuestionReviewStatus.APROBADO -> AnswerValue.APROBADO
    QuestionReviewStatus.NO_APLICA -> AnswerValue.NO_APLICA
    QuestionReviewStatus.NO_CUMPLE -> AnswerValue.NO_CUMPLE
    QuestionReviewStatus.PENDIENTE, QuestionReviewStatus.SOLICITAR_INFO -> AnswerValue.PENDIENTE
}
