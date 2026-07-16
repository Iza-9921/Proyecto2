package com.example.todoaccesible.domain.scoring

import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.Nivel

/** Proyección mínima de una pregunta del catálogo, sin dependencias de Room/Android. */
data class ScorecardQuestion(
    val codigo: String,
    val seccionId: String,
    val seccionNombre: String,
    val credito: Credito
)

data class CreditScore(
    val aprobados: Int,
    val total: Int,
    val pct: Int
)

data class SectionScore(
    val seccionId: String,
    val seccionNombre: String,
    val required: CreditScore,
    val plus: CreditScore
)

data class ScorecardResult(
    val nivel: Nivel,
    val required: CreditScore,
    val plus: CreditScore,
    val sections: List<SectionScore>
)

internal fun creditScore(aprobados: Int, total: Int): CreditScore {
    // Si no hay preguntas aplicables en este crédito, no hay nada pendiente:
    // se considera 100% cumplido (mismo criterio que excluir NA del cálculo).
    val pct = if (total == 0) 100 else Math.round(aprobados * 100.0 / total).toInt()
    return CreditScore(aprobados = aprobados, total = total, pct = pct)
}
