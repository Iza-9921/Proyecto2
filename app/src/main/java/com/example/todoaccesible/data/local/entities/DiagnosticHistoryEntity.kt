package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.DiagnosticStatus

/** Registro de auditoría: un cambio de estado (envío, revisión o validación) de un diagnóstico. */
data class DiagnosticHistoryEntity(
    val id: Long = 0,
    val diagnosticId: Long,
    val previousStatus: DiagnosticStatus?,
    val newStatus: DiagnosticStatus,
    val reviewerId: Long?,
    val comentario: String = "",
    val fecha: Long
)
