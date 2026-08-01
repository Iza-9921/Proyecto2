package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Nivel

data class DiagnosticEntity(
    val id: Long = 0,
    val clienteId: Long,
    val projectName: String,
    val ubicacion: String,
    val responsable: String,
    val revision: String,
    val fechaCreacion: Long,
    val fechaEnvio: Long? = null,
    val estado: DiagnosticStatus = DiagnosticStatus.BORRADOR,
    // Caché del último cálculo del scorecard, se recalcula al leer pero se
    // guarda para listar rápido en dashboard/kanban sin recorrer respuestas.
    val nivel: Nivel? = null,
    val requeridoPct: Int? = null,
    val plusPct: Int? = null,
    // Datos ampliados del registro de la empresa/inmueble a evaluar.
    val clienteNombre: String = "",
    val telefono: String = "",
    val entidadFederativa: String = "",
    val ciudad: String = "",
    val tipoInmueble: String = "",
    val fechaEvaluacion: Long? = null,
    // Resultado oficial: se llena solo cuando el administrador finaliza la
    // evaluación (basado en QuestionReviewEntity, no en las respuestas del
    // cliente). Separado de nivel/requeridoPct/plusPct para que el
    // recálculo preliminar del cliente nunca lo sobreescriba.
    val fechaValidacion: Long? = null,
    val observacionesAdmin: String? = null,
    val validadoPorNombre: String? = null,
    val nivelOficial: Nivel? = null,
    val requeridoPctOficial: Int? = null,
    val plusPctOficial: Int? = null
)
