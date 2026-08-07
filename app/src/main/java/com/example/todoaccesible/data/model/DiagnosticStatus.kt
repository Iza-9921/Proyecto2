package com.example.todoaccesible.data.model

/**
 * Estados del ciclo de vida de un diagnóstico: BORRADOR, PENDIENTE, EN_REVISION,
 * INFO_REQUERIDA, RECHAZADO, VALIDADO.
 */
enum class DiagnosticStatus(val label: String) {
    BORRADOR("Borrador"),
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    INFO_REQUERIDA("Información requerida"),
    RECHAZADO("Rechazado"),
    VALIDADO("Validado")
}
