package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.PhotoEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.domain.scoring.ScorecardResult
import kotlinx.coroutines.flow.Flow

interface DiagnosticRepository {
    /** Devuelve el borrador en progreso del cliente o crea uno nuevo vacío. */
    suspend fun getOrCreateDraft(clienteId: Long): DiagnosticEntity

    suspend fun updateProjectInfo(
        diagnosticId: Long,
        projectName: String,
        ubicacion: String,
        responsable: String,
        revision: String,
        clienteNombre: String = "",
        telefono: String = "",
        entidadFederativa: String = "",
        ciudad: String = "",
        tipoInmueble: String = "",
        fechaEvaluacion: Long? = null
    )

    fun observeAnswers(diagnosticId: Long): Flow<List<AnswerEntity>>
    suspend fun saveAnswer(diagnosticId: Long, codigo: String, valor: AnswerValue?, comentario: String)

    fun observePhotos(answerId: Long): Flow<List<PhotoEntity>>
    suspend fun getPhotosForAnswers(answerIds: List<Long>): Map<Long, List<PhotoEntity>>
    suspend fun addPhoto(diagnosticId: Long, codigo: String, uriPath: String): Long
    suspend fun deletePhoto(photoId: Long)

    /** Recalcula el scorecard con `ScorecardCalculator` y lo cachea en la entidad. */
    suspend fun recalculateScore(diagnosticId: Long): ScorecardResult?

    /** Cuántas de las preguntas del catálogo aún no tienen respuesta. 0 = cuestionario completo. */
    suspend fun countUnanswered(diagnosticId: Long): Int

    suspend fun submit(diagnosticId: Long)
    suspend fun discardDraft(diagnosticId: Long)

    fun observeForCliente(clienteId: Long): Flow<List<DiagnosticEntity>>
    fun observeAllSubmitted(): Flow<List<DiagnosticEntity>>
    fun observeById(id: Long): Flow<DiagnosticEntity?>
    suspend fun getById(id: Long): DiagnosticEntity?

    /**
     * Cambia el estado (usado por admin), notifica al cliente dueño del
     * diagnóstico y registra la acción en el historial (RF-20). [reviewerId]
     * es el id del admin que revisa; [comentario] es opcional (razón del
     * rechazo, qué información falta, etc.).
     */
    suspend fun updateStatus(id: Long, status: DiagnosticStatus, reviewerId: Long?, comentario: String = "")
}
