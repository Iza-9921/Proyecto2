package com.example.todoaccesible.data.model

/**
 * AP (Aprobado) | P (Pendiente) | NC (No cumple) | NA (No aplica).
 * NA se excluye siempre del numerador/denominador del scorecard.
 */
enum class AnswerValue(val short: String, val label: String) {
    APROBADO("AP", "Aprobado"),
    PENDIENTE("P", "Pendiente"),
    NO_CUMPLE("NC", "No cumple"),
    NO_APLICA("NA", "No aplica")
}
