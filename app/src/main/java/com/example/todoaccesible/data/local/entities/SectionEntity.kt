package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Las 8 secciones fijas del Scorecard v2.6 (id "1".."8"). */
@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val orden: Int
)
