package com.example.todoaccesible.ui.cliente.diagnostic.responder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.PhotoItem
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Una pregunta que el admin marcó "Solicitar información" durante la revisión, junto con su observación. */
data class PreguntaFlagged(val question: QuestionEntity, val comentarioAdmin: String)

data class ResponderInfoAdicionalUiState(
    val loading: Boolean = true,
    /** `false` si el diagnóstico no le pertenece al cliente, ya no está en INFO_REQUERIDA, o no hay preguntas marcadas. */
    val allowed: Boolean = true,
    val diagnostic: DiagnosticEntity? = null,
    val preguntas: List<PreguntaFlagged> = emptyList(),
    val answersByCode: Map<String, AnswerEntity> = emptyMap(),
    val photosByCode: Map<String, List<PhotoItem>> = emptyMap(),
    val submitting: Boolean = false
) {
    val puedeReenviar: Boolean get() =
        !submitting && preguntas.isNotEmpty() && preguntas.all { answersByCode[it.question.codigo]?.valor != null }
}

/** Reenvío de información solicitada por el admin, mirroring `src/pages/cliente/ResponderInfoAdicional.jsx`. */
class ResponderInfoAdicionalViewModel(
    private val diagnosticId: Long,
    private val diagnosticRepository: DiagnosticRepository,
    private val questionCatalogRepository: QuestionCatalogRepository,
    private val questionReviewRepository: QuestionReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResponderInfoAdicionalUiState())
    val uiState: StateFlow<ResponderInfoAdicionalUiState> = _uiState

    init {
        viewModelScope.launch {
            val diagnostic = diagnosticRepository.getById(diagnosticId)
            val flaggedReviews = questionReviewRepository.observeForDiagnostic(diagnosticId).first()
                .filter { it.status == QuestionReviewStatus.SOLICITAR_INFO }

            if (diagnostic == null || diagnostic.estado != DiagnosticStatus.INFO_REQUERIDA || flaggedReviews.isEmpty()) {
                _uiState.value = _uiState.value.copy(loading = false, allowed = false)
                return@launch
            }

            val tipo = diagnostic.tipoInmueble.ifBlank { "Otro" }
            val questionsByCode = questionCatalogRepository.getAllQuestions(tipo).associateBy { it.codigo }
            val preguntas = flaggedReviews.mapNotNull { review ->
                questionsByCode[review.questionCodigo]?.let { PreguntaFlagged(it, review.comentario) }
            }

            _uiState.value = _uiState.value.copy(loading = false, allowed = true, diagnostic = diagnostic, preguntas = preguntas)
        }
        viewModelScope.launch {
            diagnosticRepository.observeAnswers(diagnosticId).collect { list ->
                val answers = list.associateBy { it.questionCodigo }
                val photosByAnswer = diagnosticRepository.getPhotosForAnswers(answers.values.map { it.id })
                _uiState.value = _uiState.value.copy(
                    answersByCode = answers,
                    photosByCode = answers.mapValues { (_, answer) ->
                        photosByAnswer[answer.id].orEmpty().map { PhotoItem(it.id, Uri.parse(it.uriPath)) }
                    }
                )
            }
        }
    }

    fun selectAnswer(codigo: String, valor: AnswerValue) {
        val comentario = _uiState.value.answersByCode[codigo]?.comentario.orEmpty()
        viewModelScope.launch { diagnosticRepository.saveAnswer(diagnosticId, codigo, valor, comentario) }
    }

    fun onComentarioChange(codigo: String, value: String) {
        val valor = _uiState.value.answersByCode[codigo]?.valor
        viewModelScope.launch { diagnosticRepository.saveAnswer(diagnosticId, codigo, valor, value) }
    }

    fun onPhotoAdded(codigo: String, uri: Uri) {
        viewModelScope.launch { diagnosticRepository.addPhoto(diagnosticId, codigo, uri.toString()) }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch { diagnosticRepository.deletePhoto(photoId) }
    }

    fun reenviar(onDone: () -> Unit) {
        if (!_uiState.value.puedeReenviar) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true)
            diagnosticRepository.resubmitInfoAdicional(diagnosticId)
            _uiState.value = _uiState.value.copy(submitting = false)
            onDone()
        }
    }
}
