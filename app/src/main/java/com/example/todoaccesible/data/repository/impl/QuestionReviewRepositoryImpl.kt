package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.local.entities.QuestionReviewEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.DiagnosticoApiService
import com.example.todoaccesible.data.remote.dto.EvaluacionesRequest
import com.example.todoaccesible.data.remote.mapper.toAnswerValueBackend
import com.example.todoaccesible.data.remote.mapper.toBackend
import com.example.todoaccesible.data.remote.mapper.toDiagnosticStatus
import com.example.todoaccesible.data.remote.mapper.toReviewStatus
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import com.example.todoaccesible.domain.scoring.toAnswerValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * Deriva su estado de `GET /diagnosticos/:id`. El backend arma esa
 * respuesta según el ROL del propio token que llama: un admin recibe el
 * shape aplanado de `obtenerParaAdmin` (con `evaluaciones`/
 * `observacionesEspecialista` como campos de primer nivel); un cliente
 * recibe `{diagnostico, preguntas, respuestas, evidencias}` en vez de eso.
 * Pero `evaluaciones`/`observaciones_especialista` son columnas crudas de la
 * tabla `diagnosticos` (confirmado en `types/index.ts` -> `Diagnostico` y
 * `diagnostico.repository.ts::findById`, que hace `SELECT *`), así que
 * también llegan dentro de `diagnostico` en el shape cliente — solo hay que
 * leerlas de ahí en vez de asumir que no existen para ese rol.
 */
class QuestionReviewRepositoryImpl(
    private val diagnosticoApi: DiagnosticoApiService,
    private val sessionManager: SessionManager,
    private val toastController: ToastController
) : QuestionReviewRepository {

    /**
     * `var` (no parámetro de constructor) para romper el ciclo con
     * `DiagnosticRepositoryImpl`, que a su vez recibe este repositorio en su
     * propio constructor (ver `AppContainer`). Traduce el alias local
     * (negativo mientras el diagnóstico es un borrador sin sincronizar, o
     * reutilizado de una sesión anterior con otro rol en la misma instalación)
     * al id real del backend antes de cualquier llamada de red; el id sin
     * resolver solo se usa como clave del caché local en memoria (`_byDiagnostic`).
     */
    var resolveDiagnosticId: (Long) -> Long = { it }

    private val _byDiagnostic = MutableStateFlow<Map<Long, List<QuestionReviewEntity>>>(emptyMap())

    // Antes solo se refrescaba la primera vez que se pedía este diagnosticId (con un
    // set "loaded"), así que si el cliente ya había abierto el detalle antes de que el
    // admin pidiera información adicional, esta pantalla seguía sirviendo el snapshot
    // viejo (sin ninguna pregunta marcada "solicitar_info") y ResponderInfoAdicional
    // se cerraba solo por creer que no había nada que responder. Ahora se refresca cada
    // vez que algo empieza a observar este diagnóstico, igual que el diagnóstico mismo
    // (ver DiagnosticRepositoryImpl.observeById/getById).
    override fun observeForDiagnostic(diagnosticId: Long): Flow<List<QuestionReviewEntity>> =
        _byDiagnostic
            .onStart { runCatching { refresh(diagnosticId) } }
            .map { it[diagnosticId].orEmpty() }

    private suspend fun refresh(diagnosticId: Long) {
        val realId = resolveDiagnosticId(diagnosticId)
        if (realId < 0) return // borrador que todavía no existe en el backend: nada que traer.
        val esAdmin = sessionManager.session.first()?.rol == Role.ADMIN
        val (evaluaciones, observaciones, estadoBackend) = if (esAdmin) {
            val dto = diagnosticoApi.obtenerParaAdmin(realId)
            Triple(dto.evaluaciones.orEmpty(), dto.observacionesEspecialista.orEmpty(), dto.status)
        } else {
            val dto = diagnosticoApi.obtenerParaCliente(realId)
            Triple(dto.diagnostico.evaluaciones.orEmpty(), dto.diagnostico.observaciones_especialista.orEmpty(), dto.diagnostico.estado)
        }
        // El backend no tiene un valor de `evaluaciones` para "solicitar información": al guardarlo
        // (ver EnumMappers.toBackend) se guarda como "pendiente" igual que una pregunta nunca revisada,
        // así que ese estado se pierde en el viaje de ida y vuelta y ResponderInfoAdicionalViewModel
        // nunca encontraba preguntas marcadas (el botón del cliente navegaba y de inmediato regresaba).
        // Se reconstruye: una pregunta nunca tocada no aparece en `evaluaciones` en absoluto, así que
        // un "pendiente" presente ahí mientras el diagnóstico sigue en info_requerida solo puede venir
        // de haberla marcado "Solicitar información" (las demás validaciones sí distinguen su propio
        // valor). OJO: no se exige comentario por pregunta -- el admin puede solicitar información con
        // solo el mensaje general (`observaciones["general"]`), sin anotar cada pregunta una por una;
        // ese mensaje general se usa como respaldo cuando no hay uno específico para la pregunta.
        val infoRequerida = estadoBackend.toDiagnosticStatus() == DiagnosticStatus.INFO_REQUERIDA
        val comentarioGeneral = observaciones["general"].orEmpty()
        val list = evaluaciones.map { (codigo, valorStr) ->
            val comentario = observaciones[codigo].orEmpty()
            val valor = valorStr.toAnswerValueBackend() ?: AnswerValue.PENDIENTE
            val status = if (infoRequerida && valor == AnswerValue.PENDIENTE) {
                QuestionReviewStatus.SOLICITAR_INFO
            } else {
                valor.toReviewStatus()
            }
            QuestionReviewEntity(
                id = questionReviewLocalId(diagnosticId, codigo),
                diagnosticId = diagnosticId,
                questionCodigo = codigo,
                status = status,
                comentario = comentario.ifBlank { if (status == QuestionReviewStatus.SOLICITAR_INFO) comentarioGeneral else "" }
            )
        }
        _byDiagnostic.update { it + (diagnosticId to list) }
    }

    override suspend fun setStatus(diagnosticId: Long, questionCodigo: String, status: QuestionReviewStatus, reviewerId: Long) {
        upsert(diagnosticId, questionCodigo) { it.copy(status = status, reviewerId = reviewerId, fecha = System.currentTimeMillis()) }
        sync(diagnosticId)
    }

    override suspend fun setComentario(diagnosticId: Long, questionCodigo: String, comentario: String, reviewerId: Long) {
        upsert(diagnosticId, questionCodigo) { it.copy(comentario = comentario, reviewerId = reviewerId, fecha = System.currentTimeMillis()) }
        // El backend no tiene un campo de comentario por pregunta en `evaluaciones` (solo el valor); se
        // mantiene local para la UI y no se sincroniza aparte (no hay endpoint dedicado para esto).
    }

    /** Puramente local (spec): el backend no tiene una llamada dedicada para "reabrir" una pregunta. */
    override suspend fun resetForResubmission(diagnosticId: Long, questionCodigo: String) {
        upsert(diagnosticId, questionCodigo) {
            it.copy(status = QuestionReviewStatus.PENDIENTE, comentario = "", reviewerId = null, fecha = System.currentTimeMillis())
        }
    }

    private fun upsert(diagnosticId: Long, questionCodigo: String, update: (QuestionReviewEntity) -> QuestionReviewEntity) {
        _byDiagnostic.update { map ->
            val list = map[diagnosticId].orEmpty()
            val existing = list.find { it.questionCodigo == questionCodigo }
            val base = existing ?: QuestionReviewEntity(
                id = questionReviewLocalId(diagnosticId, questionCodigo),
                diagnosticId = diagnosticId,
                questionCodigo = questionCodigo
            )
            map + (diagnosticId to (list.filterNot { it.questionCodigo == questionCodigo } + update(base)))
        }
    }

    private suspend fun sync(diagnosticId: Long) {
        try {
            val realId = resolveDiagnosticId(diagnosticId)
            if (realId < 0) return // borrador que todavía no existe en el backend: nada que sincronizar.
            val evaluaciones = _byDiagnostic.value[diagnosticId].orEmpty()
                .associate { it.questionCodigo to it.status.toAnswerValue().toBackend() }
            if (evaluaciones.isEmpty()) return
            diagnosticoApi.guardarEvaluaciones(realId, EvaluacionesRequest(evaluaciones))
        } catch (e: Exception) {
            val mapped = ApiErrorMapper.handle(e, sessionManager)
            toastController.show(mapped.message, ToastTipo.ERROR)
        }
    }

    private fun questionReviewLocalId(diagnosticId: Long, codigo: String): Long =
        kotlin.math.abs("$diagnosticId#$codigo".hashCode().toLong()).let { if (it == 0L) 1L else it }
}
