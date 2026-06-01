package com.example.todoaccesible.data.remote.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val company: String? = null
)
