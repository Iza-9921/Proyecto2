package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val companyName: String,
    val projectName: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val comments: String,
    val requestDate: String
)
