package com.example.todoaccesible.data.model

import android.net.Uri

data class Question(
    val id: Int,
    val text: String,
    var answer: String? = null,
    var photoUri: Uri? = null
)
