package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.local.seed.QuestionCatalogSeeder
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import java.util.concurrent.atomic.AtomicLong

/** Catálogo sembrado por defecto al construirse (no hay backend ni base de datos). */
class QuestionCatalogRepositoryImpl(
    private val sections: InMemoryTable<SectionEntity> = InMemoryTable(QuestionCatalogSeeder.sectionEntities()),
    private val questions: InMemoryTable<QuestionEntity> = InMemoryTable(QuestionCatalogSeeder.questionEntities())
) : QuestionCatalogRepository {

    /** Sigue la numeración de las categorías sembradas ("1".."8") para las que cree el admin desde "Gestión del cuestionario". */
    private val sectionIdSeq = AtomicLong(sections.snapshot.mapNotNull { it.id.toLongOrNull() }.maxOrNull() ?: 0L)

    /** Sufijo único para el codigo de las preguntas que cree el admin (no colisiona con los sembrados "01".."38"). */
    private val questionIdSeq = AtomicLong(questions.snapshot.size.toLong())

    override fun observeSections() = sections.flow

    override suspend fun getAllSections() = sections.snapshot.sortedBy { it.orden }

    override fun observeQuestions() = questions.flow

    override suspend fun getAllQuestions() = questions.snapshot.sortedBy { it.orden }

    override suspend fun getQuestionsForSection(sectionId: String) =
        questions.snapshot.filter { it.seccionId == sectionId }.sortedBy { it.orden }

    override suspend fun updateQuestion(question: QuestionEntity) {
        questions.mutate { list -> list.map { if (it.codigo == question.codigo) question else it } }
    }

    override suspend fun getActiveSections() =
        sections.snapshot.filter { it.activa }.sortedBy { it.orden }

    override suspend fun getActiveQuestions(): List<QuestionEntity> {
        val activeSectionIds = sections.snapshot.filter { it.activa }.map { it.id }.toSet()
        return questions.snapshot
            .filter { it.activa && it.seccionId in activeSectionIds }
            .sortedBy { it.orden }
    }

    override suspend fun createSection(nombre: String): SectionEntity {
        val id = sectionIdSeq.incrementAndGet().toString()
        val orden = (sections.snapshot.maxOfOrNull { it.orden } ?: -1) + 1
        val section = SectionEntity(id = id, nombre = nombre, orden = orden, activa = true)
        sections.mutate { it + section }
        return section
    }

    override suspend fun renameSection(id: String, nombre: String) {
        sections.mutate { list -> list.map { if (it.id == id) it.copy(nombre = nombre) else it } }
    }

    override suspend fun setSectionActive(id: String, activa: Boolean) {
        sections.mutate { list -> list.map { if (it.id == id) it.copy(activa = activa) else it } }
    }

    override suspend fun moveSectionUp(id: String) = swapSectionWithNeighbor(id, -1)

    override suspend fun moveSectionDown(id: String) = swapSectionWithNeighbor(id, 1)

    private fun swapSectionWithNeighbor(id: String, delta: Int) {
        val ordered = sections.snapshot.sortedBy { it.orden }
        val index = ordered.indexOfFirst { it.id == id }
        val neighborIndex = index + delta
        if (index == -1 || neighborIndex !in ordered.indices) return
        val current = ordered[index]
        val neighbor = ordered[neighborIndex]
        sections.mutate { list ->
            list.map {
                when (it.id) {
                    current.id -> it.copy(orden = neighbor.orden)
                    neighbor.id -> it.copy(orden = current.orden)
                    else -> it
                }
            }
        }
    }

    override suspend fun deleteSection(id: String): Boolean {
        if (questions.snapshot.any { it.seccionId == id }) return false
        sections.mutate { list -> list.filterNot { it.id == id } }
        return true
    }

    override suspend fun createQuestion(
        seccionId: String,
        concepto: String,
        descripcion: String,
        credito: Credito,
        activa: Boolean,
        imagenReferenciaUri: String?
    ): QuestionEntity {
        val codigo = "$seccionId.q${questionIdSeq.incrementAndGet()}"
        val orden = (questions.snapshot.maxOfOrNull { it.orden } ?: -1) + 1
        val question = QuestionEntity(
            codigo = codigo,
            seccionId = seccionId,
            concepto = concepto,
            credito = credito,
            admiteFoto = true,
            orden = orden,
            descripcion = descripcion,
            activa = activa,
            imagenReferenciaUri = imagenReferenciaUri
        )
        questions.mutate { it + question }
        return question
    }

    override suspend fun deleteQuestion(codigo: String) {
        questions.mutate { list -> list.filterNot { it.codigo == codigo } }
    }

    override suspend fun moveQuestionUp(codigo: String) = swapQuestionWithNeighbor(codigo, -1)

    override suspend fun moveQuestionDown(codigo: String) = swapQuestionWithNeighbor(codigo, 1)

    /** Solo cambia de posición dentro de la misma categoría (no altera el orden de otras secciones). */
    private fun swapQuestionWithNeighbor(codigo: String, delta: Int) {
        val current = questions.snapshot.find { it.codigo == codigo } ?: return
        val ordered = questions.snapshot.filter { it.seccionId == current.seccionId }.sortedBy { it.orden }
        val index = ordered.indexOfFirst { it.codigo == codigo }
        val neighborIndex = index + delta
        if (index == -1 || neighborIndex !in ordered.indices) return
        val neighbor = ordered[neighborIndex]
        questions.mutate { list ->
            list.map {
                when (it.codigo) {
                    current.codigo -> it.copy(orden = neighbor.orden)
                    neighbor.codigo -> it.copy(orden = current.orden)
                    else -> it
                }
            }
        }
    }
}
