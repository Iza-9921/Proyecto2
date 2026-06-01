package com.example.todoaccesible.data.remote.model

data class LoginResponse(
    val token: String,
    val userId: String,
    val email: String
)
