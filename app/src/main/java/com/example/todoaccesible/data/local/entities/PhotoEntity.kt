package com.example.todoaccesible.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = AnswerEntity::class,
            parentColumns = ["id"],
            childColumns = ["answerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("answerId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val answerId: Long,
    val uriPath: String
)
