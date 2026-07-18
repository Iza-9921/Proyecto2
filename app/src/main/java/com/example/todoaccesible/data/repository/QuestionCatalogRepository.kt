package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import kotlinx.coroutines.flow.Flow

interface QuestionCatalogRepository {
    fun observeSections(): Flow<List<SectionEntity>>
    suspend fun getAllSections(): List<SectionEntity>
    fun observeQuestions(): Flow<List<QuestionEntity>>
    suspend fun getAllQuestions(): List<QuestionEntity>
    suspend fun getQuestionsForSection(sectionId: String): List<QuestionEntity>
    suspend fun updateQuestion(question: QuestionEntity)
}
