package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.AnswerValue

data class AnswerEntity(
    val id: Long = 0,
    val diagnosticId: Long,
    val questionCodigo: String,
    val valor: AnswerValue? = null,
    val comentario: String = ""
)
