package com.example.todoaccesible.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.todoaccesible.data.local.dao.AnswerDao
import com.example.todoaccesible.data.local.dao.DiagnosticDao
import com.example.todoaccesible.data.local.dao.NotificationDao
import com.example.todoaccesible.data.local.dao.PhotoDao
import com.example.todoaccesible.data.local.dao.QuestionCatalogDao
import com.example.todoaccesible.data.local.dao.UserDao
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.NotificationEntity
import com.example.todoaccesible.data.local.entities.PhotoEntity
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SectionEntity::class,
        QuestionEntity::class,
        DiagnosticEntity::class,
        AnswerEntity::class,
        PhotoEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun questionCatalogDao(): QuestionCatalogDao
    abstract fun diagnosticDao(): DiagnosticDao
    abstract fun answerDao(): AnswerDao
    abstract fun photoDao(): PhotoDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "todo-accesible-db"
    }
}
