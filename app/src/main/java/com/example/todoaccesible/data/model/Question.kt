package com.example.todoaccesible.data.model

import android.net.Uri

enum class AnswerType(val points: Int?) {
    SI(100),
    PARCIALMENTE(50),
    NO(0),
    NA(null)
}

data class Question(
    val id: Int,
    val category: String,
    val text: String,
    var answer: AnswerType? = null,
    var photoUri: Uri? = null
)
