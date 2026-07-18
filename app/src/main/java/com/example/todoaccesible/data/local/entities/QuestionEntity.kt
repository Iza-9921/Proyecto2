package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.Credito

/** Pregunta del catálogo, codigo con formato "1.01", "3.24", etc. */
data class QuestionEntity(
    val codigo: String,
    val seccionId: String,
    val concepto: String,
    val credito: Credito,
    val admiteFoto: Boolean,
    val orden: Int
)
