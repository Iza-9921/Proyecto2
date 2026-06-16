package com.example.todoaccesible.data.model

enum class DiagnosticStatus(val label: String) {
    PRELIMINAR("Resultado preliminar"),
    INFO_REQUERIDA("Información adicional requerida"),
    VALIDADO("Validado")
}

data class SpecialistQuestion(
    val id: String,
    val text: String,
    val answer: String,
    val evidences: List<String> = emptyList()
)

data class SpecialistCategory(
    val id: String,
    val name: String,
    val questions: List<SpecialistQuestion>
)

data class SpecialistDiagnostic(
    val id: String,
    val projectName: String,
    val clientName: String,
    val date: String,
    var status: DiagnosticStatus
)
