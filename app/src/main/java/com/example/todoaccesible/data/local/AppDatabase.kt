package com.example.todoaccesible.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todoaccesible.data.local.dao.AnswerDao
import com.example.todoaccesible.data.local.dao.ProjectDao
import com.example.todoaccesible.data.local.dao.QuotationDao
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.ProjectEntity
import com.example.todoaccesible.data.local.entities.QuotationEntity

@Database(entities = [ProjectEntity::class, AnswerEntity::class, QuotationEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun answerDao(): AnswerDao
    abstract fun quotationDao(): QuotationDao
}
