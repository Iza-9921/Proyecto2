package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoaccesible.data.model.Credito

/** Pregunta del catálogo, codigo con formato "1.01", "3.24", etc. */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["seccionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("seccionId")]
)
data class QuestionEntity(
    @PrimaryKey val codigo: String,
    val seccionId: String,
    val concepto: String,
    val credito: Credito,
    val admiteFoto: Boolean,
    val orden: Int
)
