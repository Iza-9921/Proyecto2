package com.example.todoaccesible.domain.scoring

import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.Nivel
import org.junit.Assert.assertEquals
import org.junit.Test

class ScorecardCalculatorTest {

    private fun q(codigo: String, seccionId: String, credito: Credito) =
        ScorecardQuestion(codigo, seccionId, "Sección $seccionId", credito)

    @Test
    fun `todo aprobado con plus mayor a 80 da MAGENTA`() {
        val questions = listOf(
            q("1.01", "1", Credito.REQUIRED),
            q("1.02", "1", Credito.REQUIRED),
            q("1.03", "1", Credito.PLUS),
            q("1.04", "1", Credito.PLUS),
            q("1.05", "1", Credito.PLUS)
        )
        val answers = questions.associate { it.codigo to AnswerValue.APROBADO }

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(100, result.required.pct)
        assertEquals(100, result.plus.pct)
        assertEquals(Nivel.MAGENTA, result.nivel)
    }

    @Test
    fun `requerido incompleto siempre da EN_PROCESO sin importar plus`() {
        val questions = listOf(
            q("1.01", "1", Credito.REQUIRED),
            q("1.02", "1", Credito.REQUIRED),
            q("1.03", "1", Credito.PLUS)
        )
        val answers = mapOf(
            "1.01" to AnswerValue.APROBADO,
            "1.02" to AnswerValue.NO_CUMPLE,
            "1.03" to AnswerValue.APROBADO
        )

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(50, result.required.pct)
        assertEquals(Nivel.EN_PROCESO, result.nivel)
    }

    @Test
    fun `no_aplica se excluye de numerador y denominador`() {
        val questions = listOf(
            q("1.01", "1", Credito.REQUIRED),
            q("1.02", "1", Credito.REQUIRED),
            q("1.03", "1", Credito.REQUIRED)
        )
        val answers = mapOf(
            "1.01" to AnswerValue.APROBADO,
            "1.02" to AnswerValue.APROBADO,
            "1.03" to AnswerValue.NO_APLICA
        )

        val result = ScorecardCalculator.calculate(questions, answers)

        // 2 aprobados de 2 aplicables (la NA no cuenta) -> 100%, no 66%
        assertEquals(2, result.required.total)
        assertEquals(100, result.required.pct)
    }

    @Test
    fun `plus entre 63 y 79 por ciento da ORO con requerido completo`() {
        val questions = (1..8).map { q("1.0$it", "1", Credito.PLUS) } +
            q("1.09", "1", Credito.REQUIRED)
        val answers = mutableMapOf<String, AnswerValue>()
        answers["1.09"] = AnswerValue.APROBADO
        // 5 de 8 aprobados = 62.5% -> redondea a 63%
        repeat(5) { i -> answers["1.0${i + 1}"] = AnswerValue.APROBADO }
        repeat(3) { i -> answers["1.0${i + 6}"] = AnswerValue.NO_CUMPLE }

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(63, result.plus.pct)
        assertEquals(Nivel.ORO, result.nivel)
    }

    @Test
    fun `plus por debajo de 63 por ciento con requerido completo da PLATA`() {
        val questions = listOf(
            q("1.01", "1", Credito.REQUIRED),
            q("1.02", "1", Credito.PLUS),
            q("1.03", "1", Credito.PLUS)
        )
        val answers = mapOf(
            "1.01" to AnswerValue.APROBADO,
            "1.02" to AnswerValue.APROBADO,
            "1.03" to AnswerValue.NO_CUMPLE
        )

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(50, result.plus.pct)
        assertEquals(Nivel.PLATA, result.nivel)
    }

    @Test
    fun `redondeo usa la mitad hacia arriba igual que el PDF de referencia`() {
        // 9/11 = 81.8% -> 82%, 22/23 = 95.6% -> 96% (igual que las imágenes de referencia)
        val questions = (1..11).map { q("1.0$it", "1", Credito.REQUIRED) }
        val answers = questions.mapIndexed { index, question ->
            question.codigo to if (index < 9) AnswerValue.APROBADO else AnswerValue.NO_CUMPLE
        }.toMap()

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(82, result.required.pct)
    }

    @Test
    fun `sin preguntas aplicables en un credito se considera 100 por ciento`() {
        val questions = listOf(q("1.01", "1", Credito.REQUIRED))
        val answers = mapOf("1.01" to AnswerValue.NO_APLICA)

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(0, result.required.total)
        assertEquals(100, result.required.pct)
        assertEquals(100, result.plus.pct)
        assertEquals(Nivel.MAGENTA, result.nivel)
    }

    @Test
    fun `desglose por seccion se calcula independiente del global`() {
        val questions = listOf(
            q("1.01", "1", Credito.REQUIRED),
            q("1.02", "1", Credito.REQUIRED),
            q("2.01", "2", Credito.REQUIRED)
        )
        val answers = mapOf(
            "1.01" to AnswerValue.APROBADO,
            "1.02" to AnswerValue.NO_CUMPLE,
            "2.01" to AnswerValue.APROBADO
        )

        val result = ScorecardCalculator.calculate(questions, answers)

        assertEquals(2, result.sections.size)
        assertEquals(50, result.sections.first { it.seccionId == "1" }.required.pct)
        assertEquals(100, result.sections.first { it.seccionId == "2" }.required.pct)
        // el global sigue mezclando ambas secciones: 2 de 3 -> 67%
        assertEquals(67, result.required.pct)
    }
}
