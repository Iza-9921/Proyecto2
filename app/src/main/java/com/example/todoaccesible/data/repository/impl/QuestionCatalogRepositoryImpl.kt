package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.dao.QuestionCatalogDao
import com.example.todoaccesible.data.local.seed.QuestionCatalogSeeder
import com.example.todoaccesible.data.repository.QuestionCatalogRepository

class QuestionCatalogRepositoryImpl(
    private val dao: QuestionCatalogDao
) : QuestionCatalogRepository {

    override suspend fun ensureSeeded() {
        if (dao.sectionCount() == 0) {
            dao.insertSections(QuestionCatalogSeeder.sectionEntities())
            dao.insertQuestions(QuestionCatalogSeeder.questionEntities())
        }
    }

    override fun observeSections() = dao.observeSections()

    override suspend fun getAllSections() = dao.getAllSections()

    override fun observeQuestions() = dao.observeQuestions()

    override suspend fun getAllQuestions() = dao.getAllQuestions()

    override suspend fun getQuestionsForSection(sectionId: String) = dao.getQuestionsForSection(sectionId)

    override suspend fun updateQuestion(question: com.example.todoaccesible.data.local.entities.QuestionEntity) {
        dao.updateQuestion(question)
    }
}
