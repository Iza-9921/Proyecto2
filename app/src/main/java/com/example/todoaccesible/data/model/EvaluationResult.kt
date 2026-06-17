package com.example.todoaccesible.data.model

data class EvaluationResult(
    val percentage: Int,
    val level: String,
    val countSi: Int,
    val countParcialmente: Int,
    val countNo: Int,
    val countNA: Int,
    val totalQuestions: Int
) {
    companion object {
        fun classify(percentage: Int): String {
            return when {
                percentage >= 90 -> "Excelente accesibilidad"
                percentage >= 75 -> "Buena accesibilidad"
                percentage >= 60 -> "Accesibilidad media"
                percentage >= 40 -> "Accesibilidad baja"
                else -> "Accesibilidad crítica"
            }
        }
    }
}
