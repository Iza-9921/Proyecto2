package com.example.todoaccesible.domain.scoring

import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.Nivel

/**
 * Replica exacta de la fórmula del Scorecard "Todo Accesible v2.6":
 * - NO_APLICA se excluye de numerador y denominador.
 * - requeridoPct = aprobados / total de preguntas Required (aplicables).
 * - plusPct = aprobados / total de preguntas Plus (aplicables).
 * - nivel: requeridoPct < 100 -> EN_PROCESO; si no, plusPct >= 80 -> MAGENTA;
 *          plusPct >= 63 -> ORO; si no -> PLATA.
 *
 * Nota: el pie de las tarjetas de referencia dice "Oro 65% al 79%", pero el
 * umbral que se pidió implementar es >=63%. Se usa ese valor; ajustar aquí si
 * el dueño del producto confirma que el 65% del PDF es el correcto.
 */
object ScorecardCalculator {

    fun calculate(
        questions: List<ScorecardQuestion>,
        answers: Map<String, AnswerValue?>
    ): ScorecardResult {
        val (globalRequired, globalPlus) = scoreCredits(questions, answers)

        val sections = questions
            .groupBy { it.seccionId }
            .toSortedMap()
            .map { (seccionId, sectionQuestions) ->
                val seccionNombre = sectionQuestions.first().seccionNombre
                val (required, plus) = scoreCredits(sectionQuestions, answers)
                SectionScore(seccionId, seccionNombre, required, plus)
            }

        val nivel = when {
            globalRequired.pct < 100 -> Nivel.EN_PROCESO
            globalPlus.pct >= 80 -> Nivel.MAGENTA
            globalPlus.pct >= 63 -> Nivel.ORO
            else -> Nivel.PLATA
        }

        return ScorecardResult(
            nivel = nivel,
            required = globalRequired,
            plus = globalPlus,
            sections = sections
        )
    }

    private fun scoreCredits(
        questions: List<ScorecardQuestion>,
        answers: Map<String, AnswerValue?>
    ): Pair<CreditScore, CreditScore> {
        val required = scoreForCredit(questions, answers, Credito.REQUIRED)
        val plus = scoreForCredit(questions, answers, Credito.PLUS)
        return required to plus
    }

    private fun scoreForCredit(
        questions: List<ScorecardQuestion>,
        answers: Map<String, AnswerValue?>,
        credito: Credito
    ): CreditScore {
        val applicable = questions
            .filter { it.credito == credito }
            .map { answers[it.codigo] }
            .filter { it != AnswerValue.NO_APLICA }

        val aprobados = applicable.count { it == AnswerValue.APROBADO }
        return creditScore(aprobados, applicable.size)
    }
}
