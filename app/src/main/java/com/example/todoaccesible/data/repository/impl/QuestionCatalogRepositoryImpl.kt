package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.CategoriaApiService
import com.example.todoaccesible.data.remote.dto.OrdenRequest
import com.example.todoaccesible.data.remote.dto.PreguntaRequest
import com.example.todoaccesible.data.remote.dto.SeccionRequest
import com.example.todoaccesible.data.remote.mapper.questionEntities
import com.example.todoaccesible.data.remote.mapper.toBackend
import com.example.todoaccesible.data.remote.mapper.toCredito
import com.example.todoaccesible.data.remote.mapper.toEntity
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Catálogo por tipo de inmueble, respaldado por `GET /categorias?tipo=`
 * (siempre con el query `tipo`, nunca la variante sin filtro). Cachea por
 * `tipo` en dos `StateFlow` (secciones/preguntas), sembrados on-demand la
 * primera vez que se pide un `tipo` y refrescados por completo tras cada
 * mutación (más simple y menos propenso a errores que actualizar campo por
 * campo en memoria).
 */
class QuestionCatalogRepositoryImpl(
    private val categoriaApi: CategoriaApiService,
    private val sessionManager: SessionManager,
    private val toastController: ToastController
) : QuestionCatalogRepository {

    private val _sections = MutableStateFlow<Map<String, List<SectionEntity>>>(emptyMap())
    private val _questions = MutableStateFlow<Map<String, List<QuestionEntity>>>(emptyMap())
    private val loadedTipos = ConcurrentHashMap.newKeySet<String>()

    override fun observeSections(tipo: String): Flow<List<SectionEntity>> =
        _sections.onStart { ensureLoaded(tipo) }.map { map -> map[tipo].orEmpty().sortedBy { it.orden } }

    override suspend fun getAllSections(tipo: String): List<SectionEntity> {
        ensureLoaded(tipo)
        return _sections.value[tipo].orEmpty().sortedBy { it.orden }
    }

    override fun observeQuestions(tipo: String): Flow<List<QuestionEntity>> =
        _questions.onStart { ensureLoaded(tipo) }.map { map -> map[tipo].orEmpty().sortedBy { it.orden } }

    override suspend fun getAllQuestions(tipo: String): List<QuestionEntity> {
        ensureLoaded(tipo)
        return _questions.value[tipo].orEmpty().sortedBy { it.orden }
    }

    override suspend fun getQuestionsForSection(tipo: String, sectionId: String): List<QuestionEntity> =
        getAllQuestions(tipo).filter { it.seccionId == sectionId }

    private suspend fun ensureLoaded(tipo: String) {
        if (tipo.isBlank() || !loadedTipos.add(tipo)) return
        refresh(tipo)
    }

    private suspend fun refresh(tipo: String) {
        try {
            val secciones = categoriaApi.listar(tipo).sortedBy { it.numero }
            _sections.update { it + (tipo to secciones.map { s -> s.toEntity(tipo) }) }
            _questions.update {
                val ordenadas = secciones.flatMap { s -> s.questionEntities(tipo) }
                    .mapIndexed { index, q -> q.copy(orden = index) }
                it + (tipo to ordenadas)
            }
        } catch (e: Exception) {
            loadedTipos.remove(tipo)
            reportError(e)
        }
    }

    override suspend fun updateQuestion(question: QuestionEntity) {
        val preguntaId = question.codigo.toLongOrNull() ?: return
        val seccionId = question.seccionId.toLongOrNull() ?: return
        runMutation(question.tipo) {
            categoriaApi.actualizarPregunta(
                seccionId, preguntaId,
                PreguntaRequest(
                    concepto = question.concepto,
                    credito = question.credito.toBackend(),
                    foto = question.admiteFoto,
                    descripcion = question.descripcion,
                    imagenEjemplo = question.imagenEjemplo
                )
            )
        }
    }

    override suspend fun addSeccion(tipo: String, icono: String, tituloLargo: String, tituloCorto: String): SectionEntity {
        val dto = categoriaApi.crearSeccion(tipo, SeccionRequest(icono = icono, tituloLargo = tituloLargo, tituloCorto = tituloCorto))
        refresh(tipo)
        return dto.toEntity(tipo)
    }

    override suspend fun updateSeccion(tipo: String, seccionId: String, icono: String, tituloLargo: String, tituloCorto: String) {
        val id = seccionId.toLongOrNull() ?: return
        runMutation(tipo) { categoriaApi.actualizarSeccion(id, SeccionRequest(icono = icono, tituloLargo = tituloLargo, tituloCorto = tituloCorto)) }
    }

    override suspend fun deleteSeccion(tipo: String, seccionId: String) {
        val id = seccionId.toLongOrNull() ?: return
        runMutation(tipo) { categoriaApi.eliminarSeccion(id) }
    }

    override suspend fun addPregunta(
        tipo: String,
        seccionId: String,
        concepto: String,
        credito: Credito,
        admiteFoto: Boolean,
        descripcion: String,
        imagenEjemplo: String?
    ): QuestionEntity {
        val id = seccionId.toLongOrNull()
            ?: return QuestionEntity(codigo = "", tipo = tipo, seccionId = seccionId, concepto = concepto, credito = credito, admiteFoto = admiteFoto, orden = 0)
        val dto = try {
            categoriaApi.crearPregunta(id, PreguntaRequest(concepto = concepto, credito = credito.toBackend(), foto = admiteFoto, descripcion = descripcion, imagenEjemplo = imagenEjemplo))
        } catch (e: Exception) {
            reportError(e)
            return QuestionEntity(codigo = "", tipo = tipo, seccionId = seccionId, concepto = concepto, credito = credito, admiteFoto = admiteFoto, orden = 0)
        }
        refresh(tipo)
        return QuestionEntity(
            codigo = dto.id.toString(), tipo = tipo, seccionId = seccionId, concepto = dto.concepto,
            credito = dto.credito.toCredito(), admiteFoto = dto.foto, orden = _questions.value[tipo].orEmpty().size,
            descripcion = dto.descripcion.orEmpty(), imagenEjemplo = dto.imagenEjemplo
        )
    }

    override suspend fun deletePregunta(tipo: String, codigo: String) {
        val question = _questions.value[tipo].orEmpty().find { it.codigo == codigo } ?: return
        val seccionId = question.seccionId.toLongOrNull() ?: return
        val preguntaId = codigo.toLongOrNull() ?: return
        runMutation(tipo) { categoriaApi.eliminarPregunta(seccionId, preguntaId) }
    }

    /**
     * Reordena de inmediato en el cache local (feedback instantáneo, igual que el `setSecciones`
     * optimista de `GestionPreguntas.jsx` en la web) y persiste después; si el backend lo rechaza,
     * [refresh] descarta el orden optimista y vuelve a la verdad del servidor.
     */
    override suspend fun reorderSections(tipo: String, orderedIds: List<String>) {
        val current = _sections.value[tipo].orEmpty().associateBy { it.id }
        val reordered = orderedIds.mapIndexedNotNull { index, id -> current[id]?.copy(orden = index + 1) }
        _sections.update { it + (tipo to reordered) }
        try {
            categoriaApi.ordenSecciones(tipo, OrdenRequest(orderedIds.mapNotNull(String::toLongOrNull)))
        } catch (e: Exception) {
            reportError(e)
            refresh(tipo)
        }
    }

    override suspend fun reorderQuestions(tipo: String, seccionId: String, orderedIds: List<String>) {
        val id = seccionId.toLongOrNull() ?: return
        val current = _questions.value[tipo].orEmpty().associateBy { it.codigo }
        val otras = _questions.value[tipo].orEmpty().filterNot { it.seccionId == seccionId }
        val reordenadas = orderedIds.mapIndexedNotNull { index, codigo -> current[codigo]?.copy(orden = index) }
        _questions.update { it + (tipo to (otras + reordenadas)) }
        try {
            categoriaApi.ordenPreguntas(id, OrdenRequest(orderedIds.mapNotNull(String::toLongOrNull)))
        } catch (e: Exception) {
            reportError(e)
            refresh(tipo)
        }
    }

    /**
     * No hay equivalente en el backend para "restaurar el catálogo de
     * ejemplo": el botón correspondiente ya no existe en `QuestionCatalogScreen`
     * (ver Fase 6). Se deja como no-op para no romper la firma de la interfaz.
     */
    override suspend fun restaurarEjemplo(tipo: String) = Unit

    private suspend fun runMutation(tipo: String, block: suspend () -> Unit) {
        try {
            block()
            refresh(tipo)
        } catch (e: Exception) {
            reportError(e)
        }
    }

    private suspend fun reportError(e: Exception) {
        val mapped = ApiErrorMapper.handle(e, sessionManager)
        toastController.show(mapped.message, ToastTipo.ERROR)
    }
}
