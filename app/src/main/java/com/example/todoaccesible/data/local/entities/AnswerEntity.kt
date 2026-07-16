package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoaccesible.data.model.AnswerValue

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = DiagnosticEntity::class,
            parentColumns = ["id"],
            childColumns = ["diagnosticId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["diagnosticId", "questionCodigo"], unique = true)]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diagnosticId: Long,
    val questionCodigo: String,
    val valor: AnswerValue? = null,
    val comentario: String = ""
)
