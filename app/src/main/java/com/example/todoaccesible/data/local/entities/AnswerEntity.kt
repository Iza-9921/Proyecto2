package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: String,
    val questionId: Int,
    val answer: String,
    val photoUri: String? = null
)
