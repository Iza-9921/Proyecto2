package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito
import kotlinx.coroutines.flow.Flow

interface QuestionCatalogRepository {
    fun observeSections(): Flow<List<SectionEntity>>
    suspend fun getAllSections(): List<SectionEntity>
    fun observeQuestions(): Flow<List<QuestionEntity>>
    suspend fun getAllQuestions(): List<QuestionEntity>
    suspend fun getQuestionsForSection(sectionId: String): List<QuestionEntity>
    suspend fun updateQuestion(question: QuestionEntity)

    /** Solo las categorías activas, en orden. Úsalo para lo que ve/responde el cliente (cuestionario nuevo y scorecard). */
    suspend fun getActiveSections(): List<SectionEntity>
    /** Solo las preguntas de categorías activas. */
    suspend fun getActiveQuestions(): List<QuestionEntity>

    suspend fun createSection(nombre: String): SectionEntity
    suspend fun renameSection(id: String, nombre: String)
    suspend fun setSectionActive(id: String, activa: Boolean)
    suspend fun moveSectionUp(id: String)
    suspend fun moveSectionDown(id: String)
    /** @return `false` si no se pudo eliminar porque la categoría todavía tiene preguntas asociadas. */
    suspend fun deleteSection(id: String): Boolean

    suspend fun createQuestion(
        seccionId: String,
        concepto: String,
        descripcion: String,
        credito: Credito,
        activa: Boolean,
        imagenReferenciaUri: String?
    ): QuestionEntity

    suspend fun deleteQuestion(codigo: String)
    suspend fun moveQuestionUp(codigo: String)
    suspend fun moveQuestionDown(codigo: String)
}
