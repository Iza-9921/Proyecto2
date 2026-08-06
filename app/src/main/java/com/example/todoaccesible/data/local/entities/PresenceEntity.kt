package com.example.todoaccesible.data.local.entities

/** Último "latido" de un admin viendo un diagnóstico, para el indicador "X está revisando esto". */
data class PresenceEntity(
    val diagnosticId: Long,
    val userId: Long,
    val userName: String,
    val timestampMs: Long
)
