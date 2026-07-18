package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.Role

data class UserEntity(
    val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val nombre: String,
    val rol: Role,
    val licenseActive: Boolean = true
)
