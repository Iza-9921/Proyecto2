package com.example.todoaccesible.data.model

/**
 * Columnas del Kanban de admin: PENDIENTE, EN_REVISION, INFO_REQUERIDA, VALIDADO.
 * RECHAZADO se muestra agrupado dentro de la columna INFO_REQUERIDA (ambos
 * requieren acción del cliente) pero se conserva como estado propio para
 * poder distinguirlo con una etiqueta en la UI.
 */
enum class DiagnosticStatus(val label: String) {
    BORRADOR("Borrador"),
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    INFO_REQUERIDA("Información requerida"),
    RECHAZADO("Rechazado"),
    VALIDADO("Validado")
}
