package com.example.todoaccesible.data.model

/**
 * Validación del administrador para una pregunta individual dentro de la
 * revisión detallada de un diagnóstico. Es independiente del [AnswerValue]
 * que eligió el cliente al responder: aquí es el administrador quien
 * dictamina si la respuesta/evidencia del cliente cumple el criterio.
 */
enum class QuestionReviewStatus(val label: String) {
    APROBADO("Aprobado"),
    PENDIENTE("Pendiente"),
    NO_APLICA("No aplica"),
    SOLICITAR_INFO("Solicitar información"),
    NO_CUMPLE("No cumple")
}
