package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Nivel

@Entity(tableName = "diagnostics")
data class DiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val plusPct: Int? = null
)
