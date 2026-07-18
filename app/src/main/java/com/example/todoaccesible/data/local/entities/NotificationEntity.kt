package com.example.todoaccesible.data.local.entities

/**
 * Sustituto local de los eventos de Socket.IO (nuevo_diagnostico, validado,
 * info_requerida) mientras no exista backend. Mismo modelo que consumirá el
 * listener del socket más adelante: solo cambia quién produce la fila.
 */
data class NotificationEntity(
    val id: Long = 0,
    val diagnosticId: Long,
    val destinatarioId: Long,
    val tipo: String, // "nuevo_diagnostico" | "validado" | "info_requerida"
    val mensaje: String,
    val leido: Boolean = false,
    val fecha: Long
)
