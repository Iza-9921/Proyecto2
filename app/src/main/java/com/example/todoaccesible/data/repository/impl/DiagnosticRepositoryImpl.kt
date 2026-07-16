package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.dao.AnswerDao
import com.example.todoaccesible.data.local.dao.DiagnosticDao
import com.example.todoaccesible.data.local.dao.PhotoDao
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.PhotoEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.domain.scoring.ScorecardCalculator
import com.example.todoaccesible.domain.scoring.ScorecardQuestion
import com.example.todoaccesible.domain.scoring.ScorecardResult

class DiagnosticRepositoryImpl(
    private val diagnosticDao: DiagnosticDao,
    private val answerDao: AnswerDao,
    private val photoDao: PhotoDao,
    private val questionCatalogRepository: QuestionCatalogRepository,
    private val notificationRepository: NotificationRepository
) : DiagnosticRepository {

    override suspend fun getOrCreateDraft(clienteId: Long): DiagnosticEntity {
        diagnosticDao.findDraftForCliente(clienteId)?.let { return it }
        val draft = DiagnosticEntity(
            clienteId = clienteId,
            projectName = "",
            ubicacion = "",
            responsable = "",
            revision = "1",
            fechaCreacion = System.currentTimeMillis(),
            estado = DiagnosticStatus.BORRADOR
        )
        val id = diagnosticDao.insert(draft)
        return draft.copy(id = id)
    }

    override suspend fun updateProjectInfo(
        diagnosticId: Long,
        projectName: String,
        ubicacion: String,
        responsable: String,
        revision: String
    ) {
        val diagnostic = diagnosticDao.findById(diagnosticId) ?: return
        diagnosticDao.update(
            diagnostic.copy(
                projectName = projectName,
                ubicacion = ubicacion,
                responsable = responsable,
                revision = revision
            )
        )
    }

    override fun observeAnswers(diagnosticId: Long) = answerDao.observeForDiagnostic(diagnosticId)

    override suspend fun saveAnswer(diagnosticId: Long, codigo: String, valor: AnswerValue?, comentario: String) {
        val existing = answerDao.findAnswer(diagnosticId, codigo)
        if (existing != null) {
            answerDao.update(existing.copy(valor = valor, comentario = comentario))
        } else {
            answerDao.insert(
                AnswerEntity(diagnosticId = diagnosticId, questionCodigo = codigo, valor = valor, comentario = comentario)
            )
        }
    }

    override fun observePhotos(answerId: Long) = photoDao.observeForAnswer(answerId)

    override suspend fun getPhotosForAnswers(answerIds: List<Long>) =
        photoDao.getForAnswers(answerIds).groupBy { it.answerId }

    override suspend fun addPhoto(diagnosticId: Long, codigo: String, uriPath: String): Long {
        val answerId = answerDao.findAnswer(diagnosticId, codigo)?.id
            ?: answerDao.insert(AnswerEntity(diagnosticId = diagnosticId, questionCodigo = codigo))
        return photoDao.insert(PhotoEntity(answerId = answerId, uriPath = uriPath))
    }

    override suspend fun deletePhoto(photoId: Long) = photoDao.delete(photoId)

    override suspend fun recalculateScore(diagnosticId: Long): ScorecardResult? {
        val diagnostic = diagnosticDao.findById(diagnosticId) ?: return null
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
        val answers = answerDao.getForDiagnostic(diagnosticId).associate { it.questionCodigo to it.valor }
        val result = ScorecardCalculator.calculate(scorecardQuestions, answers)

        diagnosticDao.update(
            diagnostic.copy(
                nivel = result.nivel,
                requeridoPct = result.required.pct,
                plusPct = result.plus.pct
            )
        )
        return result
    }

    override suspend fun submit(diagnosticId: Long) {
        recalculateScore(diagnosticId)
        val diagnostic = diagnosticDao.findById(diagnosticId) ?: return
        diagnosticDao.update(
            diagnostic.copy(estado = DiagnosticStatus.PENDIENTE, fechaEnvio = System.currentTimeMillis())
        )
    }

    override suspend fun discardDraft(diagnosticId: Long) = diagnosticDao.delete(diagnosticId)

    override fun observeForCliente(clienteId: Long) = diagnosticDao.observeForCliente(clienteId)

    override fun observeAllSubmitted() = diagnosticDao.observeAllSubmitted()

    override fun observeById(id: Long) = diagnosticDao.observeById(id)

    override suspend fun getById(id: Long) = diagnosticDao.findById(id)

    override suspend fun updateStatus(id: Long, status: DiagnosticStatus) {
        val diagnostic = diagnosticDao.findById(id) ?: return
        diagnosticDao.update(diagnostic.copy(estado = status))

        val (tipo, mensaje) = when (status) {
            DiagnosticStatus.VALIDADO -> "validado" to "Tu diagnóstico \"${diagnostic.projectName}\" fue validado."
            DiagnosticStatus.INFO_REQUERIDA -> "info_requerida" to "Se requiere información adicional para \"${diagnostic.projectName}\"."
            DiagnosticStatus.RECHAZADO -> "info_requerida" to "Tu diagnóstico \"${diagnostic.projectName}\" fue rechazado, revisa las observaciones."
            else -> return
        }
        notificationRepository.notify(diagnosticId = id, destinatarioId = diagnostic.clienteId, tipo = tipo, mensaje = mensaje)
    }
}
