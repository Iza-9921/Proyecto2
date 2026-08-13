package com.example.todoaccesible.data.repository.impl

import android.content.Context
import android.net.Uri
import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.core.util.encodeImageToDataUri
import com.example.todoaccesible.data.local.entities.AnswerEntity
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.PhotoEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.preferences.LocalDiagnosticMetadata
import com.example.todoaccesible.data.preferences.LocalDiagnosticMetadataStore
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.AdminApiService
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.DiagnosticoApiService
import com.example.todoaccesible.data.remote.EvidenciaApiService
import com.example.todoaccesible.data.remote.ProyectoApiService
import com.example.todoaccesible.data.remote.dto.AprobarRequest
import com.example.todoaccesible.data.remote.dto.DiagnosticoAdminDetailDto
import com.example.todoaccesible.data.remote.dto.DiagnosticoClienteDetailDto
import com.example.todoaccesible.data.remote.dto.DiagnosticoRawDto
import com.example.todoaccesible.data.remote.dto.GuardarRespuestasRequest
import com.example.todoaccesible.data.remote.dto.GuardarRespuestasResponseDto
import com.example.todoaccesible.data.remote.dto.IniciarDiagnosticoRequest
import com.example.todoaccesible.data.remote.dto.ProyectoRequest
import com.example.todoaccesible.data.remote.dto.RechazarRequest
import com.example.todoaccesible.data.remote.dto.RespuestaInputDto
import com.example.todoaccesible.data.remote.dto.ActualizarEstadoRequest
import com.example.todoaccesible.data.remote.dto.SolicitarInfoRequest
import com.example.todoaccesible.data.remote.mapper.parseBackendDate
import com.example.todoaccesible.data.remote.mapper.parseBackendDateOrNull
import com.example.todoaccesible.data.remote.mapper.toAnswerValueBackend
import com.example.todoaccesible.data.remote.mapper.toBackend
import com.example.todoaccesible.data.remote.mapper.toDiagnosticStatus
import com.example.todoaccesible.data.remote.mapper.toLightEntity
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.domain.scoring.ScorecardCalculator
import com.example.todoaccesible.domain.scoring.ScorecardQuestion
import com.example.todoaccesible.domain.scoring.ScorecardResult
import com.example.todoaccesible.domain.scoring.toAnswerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Orquesta Proyecto + Diagnostico del backend detrás de la interfaz
 * preexistente de la app. Puntos de diseño importantes (documentados en el
 * reporte final):
 *
 * - **Alias de id local**: mientras un borrador nuevo no se ha creado en el
 *   backend (el cliente todavía está en la pantalla de "datos del proyecto"),
 *   se le asigna un id local negativo (`-clienteId`). Como
 *   `updateProjectInfo()` no puede cambiar de firma para devolver el id real,
 *   [idAliasMap] traduce ese alias al id real del backend de forma
 *   transparente para toda llamada posterior (`saveAnswer`, `submit`,
 *   `observeById`, etc.) — la UI/navegación siguen usando el id local de
 *   siempre.
 * - **Fotos**: [PhotoEntity.id] negativo = foto local aún no subida (el
 *   `uriPath` es un `content://`); positivo = foto ya conocida por el
 *   backend (`uriPath` es la URL de Cloudinary). Solo se llama de verdad a
 *   `DELETE /evidencias/:id` cuando el id viene confirmado desde una
 *   recarga `GET /diagnosticos/:id` como cliente (`confirmedEvidenciaIds`) —
 *   si la foto se subió en esta misma sesión sin recargar el detalle, borrar
 *   solo limpia el estado local (limitación documentada).
 * - **Sincronización de respuestas**: local siempre se actualiza al toque;
 *   la llamada de red se dispara con un pequeño debounce por pregunta (evita
 *   golpear la API en cada tecla del campo de comentario) y, antes de
 *   enviar/reenviar, se hace un flush inmediato de todo lo pendiente para
 *   garantizar que el backend tenga el estado completo.
 */
class DiagnosticRepositoryImpl(
    private val proyectoApi: ProyectoApiService,
    private val diagnosticoApi: DiagnosticoApiService,
    private val evidenciaApi: EvidenciaApiService,
    private val adminApi: AdminApiService,
    private val questionCatalogRepository: QuestionCatalogRepository,
    private val questionReviewRepository: QuestionReviewRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val localMetaStore: LocalDiagnosticMetadataStore,
    private val toastController: ToastController,
    private val appContext: Context
) : DiagnosticRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _diagnostics = MutableStateFlow<Map<Long, DiagnosticEntity>>(emptyMap())
    private val _answers = MutableStateFlow<Map<Long, List<AnswerEntity>>>(emptyMap())
    private val _photos = MutableStateFlow<Map<Long, List<PhotoEntity>>>(emptyMap())

    /** alias (id usado por la UI) -> id real del backend, una vez que el borrador se creó allá. */
    private val idAliasMap = ConcurrentHashMap<Long, Long>()
    private val diagnosticToProyectoId = ConcurrentHashMap<Long, Long>()

    /** ids reales que el usuario descartó ("empezar de nuevo"): se excluyen de futuras búsquedas de "borrador activo". */
    private val discardedIds = ConcurrentHashMap.newKeySet<Long>()

    /** ids de evidencia confirmados como reales (vinieron de `evidencias[]` en el detalle-cliente), ver borrado de fotos. */
    private val confirmedEvidenciaIds = ConcurrentHashMap.newKeySet<Long>()

    private val syncJobs = ConcurrentHashMap<String, Job>()

    private fun resolveId(aliasId: Long): Long = idAliasMap[aliasId] ?: aliasId

    /**
     * Inverso de [resolveId]: si [realId] ya es el destino de un alias local
     * conocido (un borrador que se creó en el backend durante esta sesión),
     * devuelve ESE alias en vez de [realId]. Usado al guardar en [_diagnostics]
     * entidades que vienen de un listado (`GET /diagnosticos`,
     * `GET /admin/diagnosticos`), donde la fila trae el id real crudo — sin
     * esto, el mismo diagnóstico terminaba guardado bajo dos llaves distintas
     * (el alias negativo Y el id real) y aparecía duplicado en el dashboard.
     */
    private fun localKeyFor(realId: Long): Long =
        idAliasMap.entries.find { it.value == realId }?.key ?: realId

    private fun answerLocalId(diagnosticId: Long, codigo: String): Long {
        val h = abs("$diagnosticId#$codigo".hashCode().toLong())
        return if (h == 0L) 1L else h
    }

    private fun pendingPhotoId(answerId: Long, uriPath: String): Long {
        val h = abs("$answerId#$uriPath".hashCode().toLong())
        return -(h + 1)
    }

    private fun remotePhotoId(url: String): Long = abs(url.hashCode().toLong()) + 1

    private fun toIsoDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(millis))

    private suspend fun reportError(e: Exception) {
        val mapped = ApiErrorMapper.handle(e, sessionManager)
        toastController.show(mapped.message, ToastTipo.ERROR)
    }

    /** Conectado desde `AppContainer` a los eventos de diagnóstico de [com.example.todoaccesible.data.remote.SocketManager]. */
    fun onSocketDiagnosticEvent(diagnosticId: Long) {
        scope.launch { refreshDiagnosticDetail(diagnosticId) }
    }

    // ---- Datos del proyecto / creación en backend ----

    override suspend fun getOrCreateDraft(clienteId: Long): DiagnosticEntity {
        _diagnostics.value.values.find { it.clienteId == clienteId && it.estado == DiagnosticStatus.BORRADOR }
            ?.let { return it }

        try {
            val response = diagnosticoApi.listar(limit = 100)
            val borrador = response.rows
                .filter { it.estado == "borrador" && it.id !in discardedIds }
                .maxByOrNull { parseBackendDate(it.created_at) }
            if (borrador != null) {
                idAliasMap[borrador.id] = borrador.id
                diagnosticToProyectoId[borrador.id] = borrador.proyecto_id
                val meta = localMetaStore.get(borrador.id)
                _diagnostics.update { it + (borrador.id to buildEntityFromRawClient(borrador, meta, borrador.id)) }
                refreshDiagnosticDetail(borrador.id)
                return _diagnostics.value[borrador.id] ?: return getPlaceholderDraft(clienteId)
            }
        } catch (_: Exception) {
            // Sin conexión o sin diagnósticos todavía: se cae al borrador local.
        }
        return getPlaceholderDraft(clienteId)
    }

    private fun getPlaceholderDraft(clienteId: Long): DiagnosticEntity {
        val aliasId = -clienteId
        _diagnostics.value[aliasId]?.let { return it }
        val draft = DiagnosticEntity(
            id = aliasId,
            clienteId = clienteId,
            projectName = "",
            ubicacion = "",
            responsable = "",
            revision = "1",
            fechaCreacion = System.currentTimeMillis(),
            estado = DiagnosticStatus.BORRADOR
        )
        _diagnostics.update { it + (aliasId to draft) }
        return draft
    }

    override suspend fun updateProjectInfo(
        diagnosticId: Long,
        projectName: String,
        ubicacion: String,
        responsable: String,
        revision: String,
        clienteNombre: String,
        telefono: String,
        entidadFederativa: String,
        ciudad: String,
        tipoInmueble: String,
        fechaEvaluacion: Long?,
        logoEmpresaUri: String?
    ) {
        val meta = LocalDiagnosticMetadata(
            projectName = projectName, ubicacion = ubicacion, entidadFederativa = entidadFederativa,
            ciudad = ciudad, tipoInmueble = tipoInmueble, fechaEvaluacion = fechaEvaluacion,
            responsable = responsable, telefono = telefono, clienteNombre = clienteNombre,
            revision = revision, logoEmpresaUri = logoEmpresaUri
        )

        val realId = resolveId(diagnosticId)
        if (realId < 0) {
            // Todavía no existe nada en el backend: se crea Proyecto + se inicia el Diagnostico.
            // OJO: no se envuelve en try/catch a propósito -- si el backend responde 403 "Licencia
            // vencida o inactiva", la excepción debe propagarse para que el ViewModel la distinga.
            val proyecto = proyectoApi.crear(
                ProyectoRequest(
                    nombre_proyecto = projectName.ifBlank { "Proyecto sin nombre" },
                    cliente = clienteNombre.ifBlank { null },
                    direccion = ubicacion.ifBlank { null },
                    ciudad = ciudad.ifBlank { null },
                    estado = entidadFederativa.ifBlank { null },
                    tipo_inmueble = tipoInmueble.ifBlank { null },
                    fecha_evaluacion = fechaEvaluacion?.let { toIsoDate(it) }
                )
            )
            val iniciado = diagnosticoApi.iniciar(IniciarDiagnosticoRequest(proyecto.id))
            idAliasMap[diagnosticId] = iniciado.id
            diagnosticToProyectoId[diagnosticId] = proyecto.id
            localMetaStore.set(diagnosticId, meta)

            val clienteIdActual = _diagnostics.value[diagnosticId]?.clienteId ?: 0L
            val entity = DiagnosticEntity(
                id = diagnosticId,
                clienteId = clienteIdActual,
                projectName = projectName,
                ubicacion = ubicacion,
                responsable = responsable,
                revision = revision,
                fechaCreacion = System.currentTimeMillis(),
                estado = DiagnosticStatus.BORRADOR,
                clienteNombre = clienteNombre,
                telefono = telefono,
                entidadFederativa = entidadFederativa,
                ciudad = ciudad,
                tipoInmueble = tipoInmueble,
                fechaEvaluacion = fechaEvaluacion,
                logoEmpresaUri = logoEmpresaUri
            )
            _diagnostics.update { it + (diagnosticId to entity) }
        } else {
            localMetaStore.set(diagnosticId, meta)
            _diagnostics.update { map ->
                map[diagnosticId]?.let { d ->
                    map + (diagnosticId to d.copy(
                        projectName = projectName, ubicacion = ubicacion, responsable = responsable,
                        revision = revision, clienteNombre = clienteNombre, telefono = telefono,
                        entidadFederativa = entidadFederativa, ciudad = ciudad, tipoInmueble = tipoInmueble,
                        fechaEvaluacion = fechaEvaluacion, logoEmpresaUri = logoEmpresaUri
                    ))
                } ?: map
            }
            val proyectoId = diagnosticToProyectoId[diagnosticId]
            if (proyectoId != null) {
                try {
                    proyectoApi.actualizar(
                        proyectoId,
                        ProyectoRequest(
                            nombre_proyecto = projectName.ifBlank { "Proyecto sin nombre" },
                            cliente = clienteNombre.ifBlank { null },
                            direccion = ubicacion.ifBlank { null },
                            ciudad = ciudad.ifBlank { null },
                            estado = entidadFederativa.ifBlank { null },
                            tipo_inmueble = tipoInmueble.ifBlank { null },
                            fecha_evaluacion = fechaEvaluacion?.let { toIsoDate(it) }
                        )
                    )
                } catch (e: Exception) {
                    reportError(e)
                }
            }
        }
    }

    // ---- Respuestas / fotos ----

    override fun observeAnswers(diagnosticId: Long): Flow<List<AnswerEntity>> =
        _answers.map { it[diagnosticId].orEmpty() }

    override suspend fun saveAnswer(diagnosticId: Long, codigo: String, valor: AnswerValue?, comentario: String) {
        _answers.update { map ->
            val list = map[diagnosticId].orEmpty()
            val id = answerLocalId(diagnosticId, codigo)
            val updated = AnswerEntity(id = id, diagnosticId = diagnosticId, questionCodigo = codigo, valor = valor, comentario = comentario)
            map + (diagnosticId to (list.filterNot { it.questionCodigo == codigo } + updated))
        }
        if (valor != null) scheduleSync(diagnosticId, codigo)
    }

    private fun scheduleSync(diagnosticId: Long, codigo: String) {
        val key = "$diagnosticId#$codigo"
        syncJobs[key]?.cancel()
        syncJobs[key] = scope.launch {
            delay(600)
            try {
                syncSingleAnswer(diagnosticId, codigo)
            } catch (e: Exception) {
                reportError(e)
            } finally {
                syncJobs.remove(key)
            }
        }
    }

    private suspend fun syncSingleAnswer(diagnosticId: Long, codigo: String) {
        val realId = resolveId(diagnosticId)
        if (realId < 0) return
        val answer = _answers.value[diagnosticId]?.find { it.questionCodigo == codigo } ?: return
        if (answer.valor == null) return
        val input = buildRespuestaInput(answer)
        val response = diagnosticoApi.guardarRespuestas(realId, GuardarRespuestasRequest(listOf(input)))
        applyGuardarRespuestasResult(diagnosticId, response)
    }

    /** Cancela los debounces pendientes de este diagnóstico y empuja TODAS las respuestas de un jalón (usado antes de enviar/reenviar). */
    private suspend fun syncAllAnswersNow(diagnosticId: Long) {
        val realId = resolveId(diagnosticId)
        if (realId < 0) return
        syncJobs.keys.filter { it.startsWith("$diagnosticId#") }.forEach { syncJobs.remove(it)?.cancel() }
        val answers = _answers.value[diagnosticId].orEmpty().filter { it.valor != null }
        if (answers.isEmpty()) return
        val inputs = answers.map { buildRespuestaInput(it) }
        val response = diagnosticoApi.guardarRespuestas(realId, GuardarRespuestasRequest(inputs))
        applyGuardarRespuestasResult(diagnosticId, response)
    }

    private suspend fun buildRespuestaInput(answer: AnswerEntity): RespuestaInputDto {
        val pending = _photos.value[answer.id].orEmpty().filterNot { it.uriPath.startsWith("http") }
        val encoded = pending.mapNotNull { photo ->
            try {
                withContext(Dispatchers.IO) { appContext.encodeImageToDataUri(Uri.parse(photo.uriPath)) }
            } catch (_: Exception) {
                null
            }
        }
        return RespuestaInputDto(
            pregunta_id = answer.questionCodigo.toLongOrNull() ?: 0L,
            respuesta = (answer.valor ?: AnswerValue.PENDIENTE).toBackend(),
            observaciones = answer.comentario.ifBlank { null },
            fotos = encoded.ifEmpty { null }
        )
    }

    private fun applyGuardarRespuestasResult(diagnosticId: Long, response: GuardarRespuestasResponseDto) {
        response.respuestas.forEach { r ->
            val codigo = r.pregunta_id.toString()
            val answerId = answerLocalId(diagnosticId, codigo)
            _photos.update { map ->
                val current = map[answerId].orEmpty()
                val remoteOnly = current.filter { it.uriPath.startsWith("http") }
                val existingUrls = remoteOnly.map { it.uriPath }.toSet()
                val newRemote = r.fotos.filterNot { it in existingUrls }
                    .map { url -> PhotoEntity(id = remotePhotoId(url), answerId = answerId, uriPath = url) }
                map + (answerId to (remoteOnly + newRemote))
            }
        }
    }

    override fun observePhotos(answerId: Long): Flow<List<PhotoEntity>> = _photos.map { it[answerId].orEmpty() }

    override suspend fun getPhotosForAnswers(answerIds: List<Long>): Map<Long, List<PhotoEntity>> =
        _photos.value.filterKeys { it in answerIds }

    override suspend fun addPhoto(diagnosticId: Long, codigo: String, uriPath: String): Long {
        val answerId = answerLocalId(diagnosticId, codigo)
        _answers.update { map ->
            val list = map[diagnosticId].orEmpty()
            if (list.any { it.questionCodigo == codigo }) map
            else map + (diagnosticId to (list + AnswerEntity(id = answerId, diagnosticId = diagnosticId, questionCodigo = codigo)))
        }
        val id = pendingPhotoId(answerId, uriPath)
        _photos.update { map -> map + (answerId to (map[answerId].orEmpty() + PhotoEntity(id, answerId, uriPath))) }
        return id
    }

    override suspend fun deletePhoto(photoId: Long) {
        val (answerId, _) = findPhoto(photoId) ?: return
        if (photoId > 0 && photoId in confirmedEvidenciaIds) {
            try {
                evidenciaApi.eliminar(photoId)
            } catch (e: Exception) {
                reportError(e)
            }
        }
        _photos.update { map -> map + (answerId to map[answerId].orEmpty().filterNot { it.id == photoId }) }
    }

    private fun findPhoto(photoId: Long): Pair<Long, PhotoEntity>? {
        _photos.value.forEach { (answerId, list) -> list.find { it.id == photoId }?.let { return answerId to it } }
        return null
    }

    // ---- Scorecard local ----

    override suspend fun recalculateScore(diagnosticId: Long): ScorecardResult? {
        val diagnostic = _diagnostics.value[diagnosticId] ?: return null
        val tipo = diagnostic.tipoInmueble.ifBlank { "Otro" }
        val questions = questionCatalogRepository.getAllQuestions(tipo)
        val sectionNameById = questionCatalogRepository.getAllSections(tipo).associate { it.id to it.nombre }
        val scorecardQuestions = questions.map {
            ScorecardQuestion(it.codigo, it.seccionId, sectionNameById[it.seccionId] ?: it.seccionId, it.credito)
        }
        val answerValues = _answers.value[diagnosticId].orEmpty().associate { it.questionCodigo to it.valor }
        val result = ScorecardCalculator.calculate(scorecardQuestions, answerValues)
        _diagnostics.update { map ->
            map[diagnosticId]?.let { d ->
                map + (diagnosticId to d.copy(nivel = result.nivel, requeridoPct = result.required.pct, plusPct = result.plus.pct))
            } ?: map
        }
        return result
    }

    override suspend fun countUnanswered(diagnosticId: Long): Int {
        val diagnostic = _diagnostics.value[diagnosticId] ?: return 0
        val tipo = diagnostic.tipoInmueble.ifBlank { "Otro" }
        val total = questionCatalogRepository.getAllQuestions(tipo).size
        val answered = _answers.value[diagnosticId].orEmpty().count { it.valor != null }
        return (total - answered).coerceAtLeast(0)
    }

    // ---- Envío / ciclo de vida ----

    override suspend fun submit(diagnosticId: Long) {
        try {
            syncAllAnswersNow(diagnosticId)
            recalculateScore(diagnosticId)
            diagnosticoApi.finalizar(resolveId(diagnosticId))
            refreshDiagnosticDetail(diagnosticId)
            // El backend no descuenta `limiteCuestionarios` automáticamente al enviar
            // (es puramente informativo, solo el admin lo ajusta vía +1/-1) y el
            // endpoint que lo haría es admin-only — llamarlo aquí como cliente
            // siempre daba 403. No hay nada que sincronizar tras un envío exitoso.
        } catch (e: Exception) {
            reportError(e)
        }
    }

    override suspend fun discardDraft(diagnosticId: Long) {
        val realId = resolveId(diagnosticId)
        if (realId > 0) discardedIds.add(realId)
        idAliasMap.remove(diagnosticId)
        diagnosticToProyectoId.remove(diagnosticId)
        val answerIds = _answers.value[diagnosticId].orEmpty().map { it.id }.toSet()
        _diagnostics.update { it - diagnosticId }
        _answers.update { it - diagnosticId }
        _photos.update { it - answerIds }
        localMetaStore.clear(diagnosticId)
        syncJobs.keys.filter { it.startsWith("$diagnosticId#") }.forEach { syncJobs.remove(it)?.cancel() }
    }

    override suspend fun draftHasProgress(diagnosticId: Long): Boolean {
        val diagnostic = _diagnostics.value[diagnosticId] ?: return false
        if (diagnostic.projectName.isNotBlank() || diagnostic.ubicacion.isNotBlank()) return true
        return _answers.value[diagnosticId].orEmpty().any { it.valor != null }
    }

    override suspend fun resubmitInfoAdicional(diagnosticId: Long) {
        try {
            syncAllAnswersNow(diagnosticId)
            val flaggedCodes = questionReviewRepository.observeForDiagnostic(diagnosticId).first()
                .filter { it.status == QuestionReviewStatus.SOLICITAR_INFO }
                .map { it.questionCodigo }
            flaggedCodes.forEach { questionReviewRepository.resetForResubmission(diagnosticId, it) }
            refreshDiagnosticDetail(diagnosticId)
        } catch (e: Exception) {
            reportError(e)
        }
    }

    // ---- Listados / detalle ----

    override fun observeForCliente(clienteId: Long): Flow<List<DiagnosticEntity>> =
        _diagnostics.onStart { runCatching { refreshListForCliente(clienteId) } }
            .map { map -> map.values.filter { it.clienteId == clienteId }.sortedByDescending { it.fechaCreacion } }

    override fun observeAllSubmitted(): Flow<List<DiagnosticEntity>> =
        _diagnostics.onStart { runCatching { refreshAllSubmitted() } }
            .map { map -> map.values.filter { it.estado != DiagnosticStatus.BORRADOR }.sortedByDescending { it.fechaEnvio ?: it.fechaCreacion } }

    override fun observeById(id: Long): Flow<DiagnosticEntity?> =
        _diagnostics.onStart { runCatching { refreshDiagnosticDetail(id) } }.map { it[id] }

    override suspend fun getById(id: Long): DiagnosticEntity? {
        runCatching { refreshDiagnosticDetail(id) }
        return _diagnostics.value[id]
    }

    private suspend fun refreshListForCliente(clienteId: Long) {
        val selfId = sessionManager.session.first()?.userId
        if (selfId == null || selfId == clienteId) {
            val response = diagnosticoApi.listar(limit = 100)
            response.rows.filter { it.id !in discardedIds }.forEach { raw ->
                val key = localKeyFor(raw.id)
                idAliasMap[key] = raw.id
                diagnosticToProyectoId[key] = raw.proyecto_id
                val meta = localMetaStore.get(key)
                _diagnostics.update { it + (key to buildEntityFromRawClient(raw, meta, key)) }
            }
        } else {
            val response = adminApi.listarDiagnosticos(mapOf("usuario_id" to clienteId.toString(), "limit" to "100"))
            response.rows.forEach { row ->
                val key = localKeyFor(row.diagnostico_id)
                _diagnostics.update { it + (key to row.toLightEntity()) }
            }
        }
    }

    private suspend fun refreshAllSubmitted() {
        val response = adminApi.listarDiagnosticos(mapOf("limit" to "200"))
        response.rows.forEach { row ->
            val key = localKeyFor(row.diagnostico_id)
            _diagnostics.update { it + (key to row.toLightEntity()) }
        }
    }

    private suspend fun refreshDiagnosticDetail(aliasId: Long) {
        val realId = resolveId(aliasId)
        if (realId < 0) return // borrador que todavía no existe en el backend: nada que traer.
        val rol = sessionManager.session.first()?.rol ?: Role.CLIENTE
        try {
            if (rol == Role.ADMIN) {
                applyAdminDetail(aliasId, diagnosticoApi.obtenerParaAdmin(realId))
            } else {
                applyClienteDetail(aliasId, diagnosticoApi.obtenerParaCliente(realId))
            }
            recalculateScore(aliasId)
        } catch (e: Exception) {
            reportError(e)
        }
    }

    private suspend fun applyClienteDetail(aliasId: Long, dto: DiagnosticoClienteDetailDto) {
        val raw = dto.diagnostico
        diagnosticToProyectoId[aliasId] = raw.proyecto_id
        val meta = localMetaStore.get(aliasId)

        val answers = dto.preguntas.map { p ->
            AnswerEntity(
                id = answerLocalId(aliasId, p.pregunta_id.toString()),
                diagnosticId = aliasId,
                questionCodigo = p.pregunta_id.toString(),
                valor = p.respuesta?.toAnswerValueBackend(),
                comentario = p.observaciones.orEmpty()
            )
        }
        _answers.update { it + (aliasId to answers) }

        val respuestaIdToCodigo = dto.preguntas.mapNotNull { p -> p.respuesta_id?.let { it to p.pregunta_id.toString() } }.toMap()
        val photosByAnswer = mutableMapOf<Long, MutableList<PhotoEntity>>()
        dto.evidencias.forEach { ev ->
            val codigo = respuestaIdToCodigo[ev.respuesta_id] ?: return@forEach
            val answerId = answerLocalId(aliasId, codigo)
            confirmedEvidenciaIds.add(ev.id)
            val url = ev.ruta_imagen ?: ev.thumbnail_url ?: return@forEach
            photosByAnswer.getOrPut(answerId) { mutableListOf() }.add(PhotoEntity(id = ev.id, answerId = answerId, uriPath = url))
        }
        if (photosByAnswer.isNotEmpty()) {
            _photos.update { current -> current + photosByAnswer.mapValues { it.value.toList() } }
        }

        val entity = buildEntityFromRawClient(raw, meta, aliasId)
        _diagnostics.update { it + (aliasId to entity) }
    }

    private fun buildEntityFromRawClient(raw: DiagnosticoRawDto, meta: LocalDiagnosticMetadata, aliasId: Long): DiagnosticEntity {
        val existing = _diagnostics.value[aliasId]
        return DiagnosticEntity(
            id = aliasId,
            clienteId = raw.usuario_id,
            projectName = raw.nombre_proyecto ?: meta.projectName,
            ubicacion = meta.ubicacion,
            responsable = meta.responsable,
            revision = meta.revision,
            fechaCreacion = parseBackendDate(raw.fecha_inicio, existing?.fechaCreacion ?: System.currentTimeMillis()),
            fechaEnvio = parseBackendDateOrNull(raw.fecha_fin),
            estado = raw.estado.toDiagnosticStatus(),
            nivel = existing?.nivel,
            requeridoPct = existing?.requeridoPct,
            plusPct = existing?.plusPct,
            clienteNombre = meta.clienteNombre,
            telefono = meta.telefono,
            entidadFederativa = meta.entidadFederativa,
            ciudad = meta.ciudad,
            tipoInmueble = meta.tipoInmueble,
            fechaEvaluacion = meta.fechaEvaluacion,
            logoEmpresaUri = meta.logoEmpresaUri,
            fechaValidacion = existing?.fechaValidacion,
            observacionesAdmin = raw.recomendaciones_finales ?: existing?.observacionesAdmin,
            validadoPorNombre = existing?.validadoPorNombre,
            nivelOficial = existing?.nivelOficial,
            requeridoPctOficial = existing?.requeridoPctOficial,
            plusPctOficial = existing?.plusPctOficial
        )
    }

    private suspend fun applyAdminDetail(aliasId: Long, dto: DiagnosticoAdminDetailDto) {
        val meta = localMetaStore.get(aliasId)
        val answers = dto.respuestas.map { (codigo, r) ->
            AnswerEntity(
                id = answerLocalId(aliasId, codigo),
                diagnosticId = aliasId,
                questionCodigo = codigo,
                valor = r.valor?.toAnswerValueBackend(),
                comentario = r.comentario.orEmpty()
            )
        }
        _answers.update { it + (aliasId to answers) }

        val photosByAnswer = mutableMapOf<Long, List<PhotoEntity>>()
        dto.respuestas.forEach { (codigo, r) ->
            if (r.fotos.isEmpty()) return@forEach
            val answerId = answerLocalId(aliasId, codigo)
            photosByAnswer[answerId] = r.fotos.map { url -> PhotoEntity(id = remotePhotoId(url), answerId = answerId, uriPath = url) }
        }
        if (photosByAnswer.isNotEmpty()) {
            _photos.update { current -> current + photosByAnswer }
        }

        val existing = _diagnostics.value[aliasId]
        val entity = DiagnosticEntity(
            id = aliasId,
            clienteId = dto.clienteId?.toLongOrNull() ?: existing?.clienteId ?: 0L,
            projectName = dto.nombre ?: meta.projectName,
            ubicacion = dto.direccion ?: meta.ubicacion,
            responsable = meta.responsable,
            revision = meta.revision,
            fechaCreacion = parseBackendDate(dto.fecha, existing?.fechaCreacion ?: System.currentTimeMillis()),
            fechaEnvio = parseBackendDateOrNull(dto.fechaEnvio),
            estado = dto.status.toDiagnosticStatus(),
            nivel = existing?.nivel,
            requeridoPct = existing?.requeridoPct,
            plusPct = existing?.plusPct,
            clienteNombre = dto.cliente ?: meta.clienteNombre,
            telefono = meta.telefono,
            entidadFederativa = meta.entidadFederativa,
            ciudad = meta.ciudad,
            tipoInmueble = dto.tipo ?: meta.tipoInmueble,
            fechaEvaluacion = meta.fechaEvaluacion,
            logoEmpresaUri = meta.logoEmpresaUri,
            fechaValidacion = if (dto.status.toDiagnosticStatus() == DiagnosticStatus.VALIDADO) existing?.fechaValidacion ?: System.currentTimeMillis() else existing?.fechaValidacion,
            observacionesAdmin = dto.motivoRechazo ?: dto.observacionesEspecialista?.values?.firstOrNull() ?: existing?.observacionesAdmin,
            validadoPorNombre = existing?.validadoPorNombre,
            nivelOficial = existing?.nivelOficial,
            requeridoPctOficial = existing?.requeridoPctOficial,
            plusPctOficial = existing?.plusPctOficial
        )
        _diagnostics.update { it + (aliasId to entity) }
    }

    // ---- Acciones de administrador ----

    override suspend fun updateStatus(id: Long, status: DiagnosticStatus, reviewerId: Long?, comentario: String) {
        val realId = resolveId(id)
        try {
            when (status) {
                DiagnosticStatus.RECHAZADO -> diagnosticoApi.rechazar(realId, RechazarRequest(motivo = comentario))
                DiagnosticStatus.INFO_REQUERIDA -> diagnosticoApi.solicitarInfo(realId, SolicitarInfoRequest(mensaje = comentario))
                DiagnosticStatus.VALIDADO -> diagnosticoApi.aprobar(
                    realId,
                    AprobarRequest(observaciones = if (comentario.isNotBlank()) mapOf("general" to comentario) else emptyMap())
                )
                else -> diagnosticoApi.actualizarEstado(realId, ActualizarEstadoRequest(estado = status.toBackend()))
            }
            refreshDiagnosticDetail(id)
        } catch (e: Exception) {
            reportError(e)
        }
    }

    override suspend fun getOfficialScore(diagnosticId: Long): ScorecardResult? {
        val diagnostic = _diagnostics.value[diagnosticId] ?: return null
        val tipo = diagnostic.tipoInmueble.ifBlank { "Otro" }
        val questions = questionCatalogRepository.getAllQuestions(tipo)
        val sectionNameById = questionCatalogRepository.getAllSections(tipo).associate { it.id to it.nombre }
        val scorecardQuestions = questions.map {
            ScorecardQuestion(it.codigo, it.seccionId, sectionNameById[it.seccionId] ?: it.seccionId, it.credito)
        }
        val reviewByCode = questionReviewRepository.observeForDiagnostic(diagnosticId).first().associate { it.questionCodigo to it.status }
        val answerValues = questions.associate { it.codigo to (reviewByCode[it.codigo]?.toAnswerValue() ?: AnswerValue.PENDIENTE) }
        return ScorecardCalculator.calculate(scorecardQuestions, answerValues)
    }

    override suspend fun finalizeOfficialScore(diagnosticId: Long, reviewerId: Long, comentario: String): ScorecardResult? {
        val result = getOfficialScore(diagnosticId) ?: return null
        try {
            diagnosticoApi.aprobar(
                resolveId(diagnosticId),
                AprobarRequest(observaciones = if (comentario.isNotBlank()) mapOf("general" to comentario) else emptyMap())
            )
            val reviewerName = userRepository.observeById(reviewerId).first()?.nombre
            _diagnostics.update { map ->
                map[diagnosticId]?.let { d ->
                    map + (diagnosticId to d.copy(
                        estado = DiagnosticStatus.VALIDADO,
                        nivelOficial = result.nivel,
                        requeridoPctOficial = result.required.pct,
                        plusPctOficial = result.plus.pct,
                        fechaValidacion = System.currentTimeMillis(),
                        observacionesAdmin = comentario.ifBlank { d.observacionesAdmin },
                        validadoPorNombre = reviewerName
                    ))
                } ?: map
            }
            refreshDiagnosticDetail(diagnosticId)
        } catch (e: Exception) {
            reportError(e)
        }
        return result
    }
}
