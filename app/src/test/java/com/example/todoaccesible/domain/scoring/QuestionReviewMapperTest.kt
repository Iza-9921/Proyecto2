package com.example.todoaccesible.domain.scoring

import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.Nivel
import com.example.todoaccesible.data.model.QuestionReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionReviewMapperTest {

    @Test
    fun `aprobado no_aplica y no_cumple se mapean 1 a 1`() {
        assertEquals(AnswerValue.APROBADO, QuestionReviewStatus.APROBADO.toAnswerValue())
        assertEquals(AnswerValue.NO_APLICA, QuestionReviewStatus.NO_APLICA.toAnswerValue())
        assertEquals(AnswerValue.NO_CUMPLE, QuestionReviewStatus.NO_CUMPLE.toAnswerValue())
    }

    @Test
    fun `pendiente y solicitar_info se mapean como pendiente`() {
        assertEquals(AnswerValue.PENDIENTE, QuestionReviewStatus.PENDIENTE.toAnswerValue())
        assertEquals(AnswerValue.PENDIENTE, QuestionReviewStatus.SOLICITAR_INFO.toAnswerValue())
    }

    @Test
    fun `una pregunta con solicitar_info deja el nivel oficial en EN_PROCESO`() {
        val questions = listOf(
            ScorecardQuestion("1.01", "1", "Sección 1", Credito.REQUIRED),
            ScorecardQuestion("1.02", "1", "Sección 1", Credito.REQUIRED)
        )
        val reviews = mapOf(
            "1.01" to QuestionReviewStatus.APROBADO,
            "1.02" to QuestionReviewStatus.SOLICITAR_INFO
        )
        val answers = reviews.mapValues { it.value.toAnswerValue() }

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(50, result.required.pct)
        assertEquals(Nivel.EN_PROCESO, result.nivel)
    }
}
