@file:OptIn(ExperimentalCoroutinesApi::class)

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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

    private val answersByCode: StateFlow<Map<String, AnswerEntity>> = diagnosticRepository
        .observeAnswers(diagnosticId)
        .map { list -> list.associateBy { it.questionCodigo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Antes se recalculaba con un fetch de una sola vez (`getPhotosForAnswers`) metido dentro del
    // collector de respuestas: como `addPhoto`/`deletePhoto` no siempre cambian `_answers` (p. ej.
    // si la pregunta ya tenía respuesta), ese collector no se volvía a disparar y la foto agregada
    // o borrada no aparecía/desaparecía en pantalla. Ahora se deriva de un flow en vivo por
    // respuesta (igual que QuestionnaireViewModel.currentPhotos), así que sí refleja cada cambio.
    private val photosByCode: StateFlow<Map<String, List<PhotoItem>>> = answersByCode.flatMapLatest { answers ->
        if (answers.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                answers.map { (codigo, answer) ->
                    diagnosticRepository.observePhotos(answer.id).map { photos ->
                        codigo to photos.map { PhotoItem(it.id, Uri.parse(it.uriPath)) }
                    }
                }
            ) { pairs -> pairs.toMap() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            val diagnostic = diagnosticRepository.getById(diagnosticId)
            val flaggedReviews = questionReviewRepository.observeForDiagnostic(diagnosticRepository.resolveId(diagnosticId)).first()
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
        combine(answersByCode, photosByCode) { answers, photos -> answers to photos }
            .onEach { (answers, photos) ->
                _uiState.value = _uiState.value.copy(answersByCode = answers, photosByCode = photos)
            }
            .launchIn(viewModelScope)
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
            val success = diagnosticRepository.resubmitInfoAdicional(diagnosticId)
            _uiState.value = _uiState.value.copy(submitting = false)
            // Si falló, el repositorio ya mostró el toast de error; no navegamos para no hacerle
            // creer al usuario que ya reenvió la información cuando en realidad no se guardó.
            if (success) onDone()
        }
    }
}
