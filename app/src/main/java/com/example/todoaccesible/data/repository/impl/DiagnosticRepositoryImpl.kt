package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.PhotoEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.local.seed.QuestionCatalogSeeder
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.domain.scoring.ScorecardCalculator
import com.example.todoaccesible.domain.scoring.ScorecardQuestion
import com.example.todoaccesible.domain.scoring.ScorecardResult
import kotlinx.coroutines.flow.map

class DiagnosticRepositoryImpl(
    private val diagnostics: InMemoryTable<DiagnosticEntity> = InMemoryTable(listOf(demoSeed.first)),
    private val answers: InMemoryTable<AnswerEntity> = InMemoryTable(demoSeed.second),
    private val photos: InMemoryTable<PhotoEntity> = InMemoryTable(),
    private val questionCatalogRepository: QuestionCatalogRepository,
    private val notificationRepository: NotificationRepository,
    private val diagnosticHistoryRepository: DiagnosticHistoryRepository
) : DiagnosticRepository {

    companion object {
        /**
         * Diagnóstico ya validado y con las 187 preguntas contestadas (todas
         * "Aprobado"), sembrado para el cliente demo. Así se puede probar de
         * inmediato la descarga del PDF sin tener que llenar el cuestionario
         * completo a mano. Se computa una sola vez (lazy) porque ambos parámetros
         * por defecto del constructor lo necesitan.
         */
        private val demoSeed: Pair<DiagnosticEntity, List<AnswerEntity>> by lazy {
            val questions = QuestionCatalogSeeder.questionEntities()
            val sectionNameById = QuestionCatalogSeeder.sectionEntities().associate { it.id to it.nombre }
            val scorecardQuestions = questions.map {
                ScorecardQuestion(
                    codigo = it.codigo,
                    seccionId = it.seccionId,
                    seccionNombre = sectionNameById[it.seccionId] ?: it.seccionId,
                    credito = it.credito
                )
            }
            val answerValues = questions.associate { it.codigo to AnswerValue.APROBADO }
            val result = ScorecardCalculator.calculate(scorecardQuestions, answerValues)

            val diagnostic = DiagnosticEntity(
                id = 1,
                clienteId = UserRepositoryImpl.DEMO_CLIENT_ID,
                projectName = "Edificio Demo Todo Accesible",
                ubicacion = "Ciudad de México",
                responsable = "Cliente Demo",
                revision = "1",
                fechaCreacion = System.currentTimeMillis(),
                fechaEnvio = System.currentTimeMillis(),
                estado = DiagnosticStatus.VALIDADO,
                nivel = result.nivel,
                requeridoPct = result.required.pct,
                plusPct = result.plus.pct
            )
            val demoAnswers = questions.mapIndexed { index, question ->
                AnswerEntity(
                    id = index + 1L,
                    diagnosticId = diagnostic.id,
                    questionCodigo = question.codigo,
                    valor = AnswerValue.APROBADO
                )
            }
            diagnostic to demoAnswers
        }
    }

    override suspend fun getOrCreateDraft(clienteId: Long): DiagnosticEntity {
        diagnostics.snapshot.find { it.clienteId == clienteId && it.estado == DiagnosticStatus.BORRADOR }
            ?.let { return it }
        val id = diagnostics.nextId()
        val draft = DiagnosticEntity(
            id = id,
            clienteId = clienteId,
            projectName = "",
            ubicacion = "",
            responsable = "",
            revision = "1",
            fechaCreacion = System.currentTimeMillis(),
            estado = DiagnosticStatus.BORRADOR
        )
        diagnostics.mutate { it + draft }
        return draft
    }

    override suspend fun updateProjectInfo(
        diagnosticId: Long,
        projectName: String,
        ubicacion: String,
        responsable: String,
        revision: String
    ) {
        diagnostics.mutate { list ->
            list.map {
                if (it.id == diagnosticId) {
                    it.copy(projectName = projectName, ubicacion = ubicacion, responsable = responsable, revision = revision)
                } else it
            }
        }
    }

    override fun observeAnswers(diagnosticId: Long) =
        answers.flow.map { list -> list.filter { it.diagnosticId == diagnosticId } }

    override suspend fun saveAnswer(diagnosticId: Long, codigo: String, valor: AnswerValue?, comentario: String) {
        val existing = answers.snapshot.find { it.diagnosticId == diagnosticId && it.questionCodigo == codigo }
        if (existing != null) {
            answers.mutate { list -> list.map { if (it.id == existing.id) it.copy(valor = valor, comentario = comentario) else it } }
        } else {
            val id = answers.nextId()
            answers.mutate {
                it + AnswerEntity(id = id, diagnosticId = diagnosticId, questionCodigo = codigo, valor = valor, comentario = comentario)
            }
        }
    }

    override fun observePhotos(answerId: Long) =
        photos.flow.map { list -> list.filter { it.answerId == answerId } }

    override suspend fun getPhotosForAnswers(answerIds: List<Long>) =
        photos.snapshot.filter { it.answerId in answerIds }.groupBy { it.answerId }

    override suspend fun addPhoto(diagnosticId: Long, codigo: String, uriPath: String): Long {
        val answerId = answers.snapshot.find { it.diagnosticId == diagnosticId && it.questionCodigo == codigo }?.id
            ?: run {
                val id = answers.nextId()
                answers.mutate { it + AnswerEntity(id = id, diagnosticId = diagnosticId, questionCodigo = codigo) }
                id
            }
        val photoId = photos.nextId()
        photos.mutate { it + PhotoEntity(id = photoId, answerId = answerId, uriPath = uriPath) }
        return photoId
    }

    override suspend fun deletePhoto(photoId: Long) {
        photos.mutate { list -> list.filterNot { it.id == photoId } }
    }

    override suspend fun recalculateScore(diagnosticId: Long): ScorecardResult? {
        val diagnostic = diagnostics.snapshot.find { it.id == diagnosticId } ?: return null
        val questions = questionCatalogRepository.getAllQuestions()
        val sectionNameById = questionCatalogRepository.getAllSections().associate { it.id to it.nombre }
        val scorecardQuestions = questions.map {
            ScorecardQuestion(
                codigo = it.codigo,
                seccionId = it.seccionId,
                seccionNombre = sectionNameById[it.seccionId] ?: it.seccionId,
                credito = it.credito
            )
        }
        val answerValues = answers.snapshot.filter { it.diagnosticId == diagnosticId }
            .associate { it.questionCodigo to it.valor }
        val result = ScorecardCalculator.calculate(scorecardQuestions, answerValues)

        diagnostics.mutate { list ->
            list.map {
                if (it.id == diagnosticId) {
                    it.copy(nivel = result.nivel, requeridoPct = result.required.pct, plusPct = result.plus.pct)
                } else it
            }
        }
        return result
    }

    override suspend fun countUnanswered(diagnosticId: Long): Int {
        val totalQuestions = questionCatalogRepository.getAllQuestions().size
        val answeredCount = answers.snapshot.count { it.diagnosticId == diagnosticId && it.valor != null }
        return (totalQuestions - answeredCount).coerceAtLeast(0)
    }

    override suspend fun submit(diagnosticId: Long) {
        recalculateScore(diagnosticId)
        val diagnostic = diagnostics.snapshot.find { it.id == diagnosticId } ?: return
        val previousStatus = diagnostic.estado
        diagnostics.mutate { list ->
            list.map {
                if (it.id == diagnosticId) {
                    it.copy(estado = DiagnosticStatus.PENDIENTE, fechaEnvio = System.currentTimeMillis())
                } else it
            }
        }
        diagnosticHistoryRepository.record(
            diagnosticId = diagnosticId,
            previousStatus = previousStatus,
            newStatus = DiagnosticStatus.PENDIENTE,
            reviewerId = null,
            comentario = "Enviado por el cliente para revisión"
        )
    }

    override suspend fun discardDraft(diagnosticId: Long) {
        diagnostics.mutate { list -> list.filterNot { it.id == diagnosticId } }
        answers.mutate { list -> list.filterNot { it.diagnosticId == diagnosticId } }
    }

    override fun observeForCliente(clienteId: Long) =
        diagnostics.flow.map { list -> list.filter { it.clienteId == clienteId }.sortedByDescending { it.fechaCreacion } }

    override fun observeAllSubmitted() =
        diagnostics.flow.map { list ->
            list.filter { it.estado != DiagnosticStatus.BORRADOR }.sortedByDescending { it.fechaEnvio }
        }

    override fun observeById(id: Long) =
        diagnostics.flow.map { list -> list.find { it.id == id } }

    override suspend fun getById(id: Long) = diagnostics.snapshot.find { it.id == id }

    override suspend fun updateStatus(id: Long, status: DiagnosticStatus, reviewerId: Long?, comentario: String) {
        val diagnostic = diagnostics.snapshot.find { it.id == id } ?: return
        val previousStatus = diagnostic.estado
        diagnostics.mutate { list -> list.map { if (it.id == id) it.copy(estado = status) else it } }

        diagnosticHistoryRepository.record(
            diagnosticId = id,
            previousStatus = previousStatus,
            newStatus = status,
            reviewerId = reviewerId,
            comentario = comentario
        )

        val (tipo, mensaje) = when (status) {
            DiagnosticStatus.VALIDADO -> "validado" to "Tu diagnóstico \"${diagnostic.projectName}\" fue validado."
            DiagnosticStatus.INFO_REQUERIDA -> "info_requerida" to "Se requiere información adicional para \"${diagnostic.projectName}\"."
            DiagnosticStatus.RECHAZADO -> "info_requerida" to "Tu diagnóstico \"${diagnostic.projectName}\" fue rechazado, revisa las observaciones."
            else -> return
        }
        notificationRepository.notify(diagnosticId = id, destinatarioId = diagnostic.clienteId, tipo = tipo, mensaje = mensaje)
    }
}
