package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoaccesible.data.model.Role

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val nombre: String,
    val rol: Role
)
