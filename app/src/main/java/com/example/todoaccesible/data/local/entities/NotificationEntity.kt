package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sustituto local de los eventos de Socket.IO (nuevo_diagnostico, validado,
 * info_requerida) mientras no exista backend. Mismo modelo que consumirá el
 * listener del socket más adelante: solo cambia quién produce la fila.
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = DiagnosticEntity::class,
            parentColumns = ["id"],
            childColumns = ["diagnosticId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("diagnosticId"), Index("destinatarioId")]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diagnosticId: Long,
    val destinatarioId: Long,
    val tipo: String, // "nuevo_diagnostico" | "validado" | "info_requerida"
    val mensaje: String,
    val leido: Boolean = false,
    val fecha: Long
)
