package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.local.seed.QuestionCatalogSeeder
import com.example.todoaccesible.data.repository.QuestionCatalogRepository

/** Catálogo sembrado por defecto al construirse (no hay backend ni base de datos). */
class QuestionCatalogRepositoryImpl(
    private val sections: InMemoryTable<SectionEntity> = InMemoryTable(QuestionCatalogSeeder.sectionEntities()),
    private val questions: InMemoryTable<QuestionEntity> = InMemoryTable(QuestionCatalogSeeder.questionEntities())
) : QuestionCatalogRepository {

    override fun observeSections() = sections.flow

    override suspend fun getAllSections() = sections.snapshot.sortedBy { it.orden }

    override fun observeQuestions() = questions.flow

    override suspend fun getAllQuestions() = questions.snapshot.sortedBy { it.orden }

    override suspend fun getQuestionsForSection(sectionId: String) =
        questions.snapshot.filter { it.seccionId == sectionId }.sortedBy { it.orden }

    override suspend fun updateQuestion(question: QuestionEntity) {
        questions.mutate { list -> list.map { if (it.codigo == question.codigo) question else it } }
    }
}
