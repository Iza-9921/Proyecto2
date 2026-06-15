package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val responsible: String,
    val description: String = "",
    val status: String = "Diagnóstico en proceso",
    val lastEvaluationDate: String,
    val compliancePercentage: Int = 0
)
