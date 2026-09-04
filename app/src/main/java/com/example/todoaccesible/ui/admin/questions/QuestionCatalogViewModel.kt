package com.example.todoaccesible.ui.admin.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Todos los diálogos/modales de la pantalla, mirroring los estados de `GestionPreguntas.jsx`. */
sealed class QuestionCatalogDialog {
    data class EditSection(val section: SectionEntity) : QuestionCatalogDialog()
    data class DeleteSection(val section: SectionEntity) : QuestionCatalogDialog()
    object AddSection : QuestionCatalogDialog()
    data class EditQuestion(val question: QuestionEntity) : QuestionCatalogDialog()
    data class DeleteQuestion(val question: QuestionEntity) : QuestionCatalogDialog()
    data class AddQuestion(val seccionId: String) : QuestionCatalogDialog()
    object CreateTipo : QuestionCatalogDialog()
    object DeleteTipo : QuestionCatalogDialog()
    object RestaurarEjemplo : QuestionCatalogDialog()
    object RenombrarTipo : QuestionCatalogDialog()
    object DuplicarTipo : QuestionCatalogDialog()
    data class ElegirPreguntas(val seccionId: String) : QuestionCatalogDialog()
}

/** Resultado del banco de preguntas: la pregunta más de dónde viene, para el picker "Elegir preguntas". */
data class BancoPregunta(
    val question: QuestionEntity,
    val origenTipo: String,
    val origenSeccion: String
)

data class QuestionCatalogUiState(
    val tipos: List<String> = emptyList(),
    val selectedTipo: String = "",
    val sections: List<SectionEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val expandedSections: Set<String> = emptySet(),
    val dialog: QuestionCatalogDialog? = null,
    /** Mientras crear/renombrar/duplicar cuestionario está en vuelo; el diálogo lo usa para mostrar "Guardando…"/"Duplicando…" y bloquear un segundo submit. */
    val tipoActionBusy: Boolean = false
) {
    /** No se permite eliminar el último cuestionario restante, igual que en la web. */
    val puedeEliminarTipo: Boolean get() = tipos.size > 1

    fun preguntasDe(seccionId: String): List<QuestionEntity> =
        questions.filter { it.seccionId == seccionId }.sortedBy { it.orden }
}

/**
 * CRUD completo de cuestionarios (tipos de inmueble), secciones y
 * preguntas, mirroring `src/pages/admin/GestionPreguntas.jsx` en la web.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestionCatalogViewModel(
    private val catalogRepository: QuestionCatalogRepository,
    private val tipoRepository: TipoCuestionarioRepository
) : ViewModel() {

    private val _selectedTipo = MutableStateFlow("")
    private val _expandedSections = MutableStateFlow<Set<String>>(emptySet())
    private val _dialog = MutableStateFlow<QuestionCatalogDialog?>(null)

    /**
     * `TextInputDialog` no se cierra ni deshabilita su botón "Guardar" hasta que
     * `closeDialog()` corre al final de la corrutina; para "Duplicar cuestionario"
     * eso puede tardar varios segundos (una llamada HTTP secuencial por cada
     * sección/pregunta a copiar). Sin este guard, un doble tap por impaciencia
     * lanza una segunda corrutina en paralelo que borra las secciones recién
     * creadas por la primera mientras esta sigue insertando preguntas en ellas,
     * y el backend responde 500 (FK: la categoría ya no existe).
     */
    private val tipoActionInFlight = AtomicBoolean(false)
    private val _tipoActionBusy = MutableStateFlow(false)

    private val tipos: StateFlow<List<String>> = tipoRepository.observeTipos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sections: StateFlow<List<SectionEntity>> = _selectedTipo.flatMapLatest { tipo ->
        if (tipo.isBlank()) flowOf(emptyList()) else catalogRepository.observeSections(tipo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val questions: StateFlow<List<QuestionEntity>> = _selectedTipo.flatMapLatest { tipo ->
        if (tipo.isBlank()) flowOf(emptyList()) else catalogRepository.observeQuestions(tipo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class BaseState(val tipos: List<String>, val selectedTipo: String, val sections: List<SectionEntity>)
    private data class ExtraState(val questions: List<QuestionEntity>, val expanded: Set<String>, val dialog: QuestionCatalogDialog?, val busy: Boolean)

    private val base = combine(tipos, _selectedTipo, sections) { t, s, sec -> BaseState(t, s, sec) }
    private val extra = combine(questions, _expandedSections, _dialog, _tipoActionBusy) { q, e, d, busy -> ExtraState(q, e, d, busy) }

    val uiState: StateFlow<QuestionCatalogUiState> = combine(base, extra) { b, e ->
        QuestionCatalogUiState(b.tipos, b.selectedTipo, b.sections, e.questions, e.expanded, e.dialog, e.busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuestionCatalogUiState())

    init {
        viewModelScope.launch {
            tipos.collect { list ->
                if (_selectedTipo.value.isBlank() && list.isNotEmpty()) {
                    _selectedTipo.value = list.first()
                }
            }
        }
    }

    fun selectTipo(tipo: String) { _selectedTipo.value = tipo }

    fun toggleSection(seccionId: String) {
        _expandedSections.value = _expandedSections.value.toMutableSet().apply {
            if (!add(seccionId)) remove(seccionId)
        }
    }

    fun openDialog(dialog: QuestionCatalogDialog) { _dialog.value = dialog }
    fun closeDialog() { _dialog.value = null }

    /**
     * `tipoRepository.addTipo/renombrarTipo/deleteTipo` atrapan sus propias excepciones (ya
     * muestran su toast de error) y no relanzan, así que la única forma de saber si la mutación
     * de verdad ocurrió es mirar `tipos.value` después de esperarla — nunca asumir éxito, o el
     * dropdown termina "seleccionando" un tipo que nunca se creó/renombró (o que sigue existiendo
     * tras un delete fallido), igual que le pasaría a la web si su `catch` no abortara.
     */
    fun confirmCreateTipo(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty() || !tipoActionInFlight.compareAndSet(false, true)) return
        _tipoActionBusy.value = true
        viewModelScope.launch {
            try {
                tipoRepository.addTipo(limpio)
                if (limpio in tipos.value) {
                    _selectedTipo.value = limpio
                    closeDialog()
                }
            } finally {
                _tipoActionBusy.value = false
                tipoActionInFlight.set(false)
            }
        }
    }

    fun confirmDeleteTipo() {
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            tipoRepository.deleteTipo(tipo)
            if (tipo !in tipos.value) {
                _selectedTipo.value = tipos.value.firstOrNull().orEmpty()
                closeDialog()
            }
        }
    }

    fun confirmRenombrarTipo(nombreNuevo: String) {
        val limpio = nombreNuevo.trim()
        if (limpio.isEmpty() || !tipoActionInFlight.compareAndSet(false, true)) return
        val actual = _selectedTipo.value
        _tipoActionBusy.value = true
        viewModelScope.launch {
            try {
                tipoRepository.renombrarTipo(actual, limpio)
                if (limpio in tipos.value) {
                    _selectedTipo.value = limpio
                    closeDialog()
                }
            } finally {
                _tipoActionBusy.value = false
                tipoActionInFlight.set(false)
            }
        }
    }

    fun confirmDuplicarTipo(nombreNuevo: String) {
        val limpio = nombreNuevo.trim()
        if (limpio.isEmpty() || !tipoActionInFlight.compareAndSet(false, true)) return
        val tipoOrigen = _selectedTipo.value
        _tipoActionBusy.value = true
        viewModelScope.launch {
            var tipoCreado = false
            try {
                tipoRepository.addTipo(limpio)
                tipoCreado = limpio in tipos.value
                if (!tipoCreado) error("No se pudo crear el cuestionario \"$limpio\"")
                // El backend siembra todo tipo nuevo con las 8 secciones genéricas por
                // defecto (para que "Crear nuevo cuestionario" no arranque vacío). Como
                // aquí se va a duplicar la estructura real del tipo de origen, esas
                // secciones genéricas sobran: se borran antes de copiar, si no quedaban
                // mezcladas con las del origen.
                catalogRepository.getAllSections(limpio).forEach { catalogRepository.deleteSeccion(limpio, it.id) }
                val seccionesOrigen = catalogRepository.getAllSections(tipoOrigen)
                for (seccion in seccionesOrigen) {
                    val nuevaSeccion = catalogRepository.addSeccion(limpio, seccion.icono, seccion.nombre, seccion.tituloCorto)
                    val preguntas = catalogRepository.getQuestionsForSection(tipoOrigen, seccion.id)
                    for (p in preguntas) {
                        val creada = catalogRepository.addPregunta(
                            limpio, nuevaSeccion.id, p.concepto, p.credito, p.admiteFoto, p.descripcion, p.imagenEjemplo
                        )
                        // `addPregunta` traga sus propios errores de red y devuelve este sentinel
                        // (codigo vacío) en vez de relanzar; sin este chequeo el duplicado seguiría
                        // "de éxito" con preguntas faltantes en silencio.
                        if (creada.codigo.isEmpty()) error("No se pudo copiar la pregunta \"${p.concepto}\"")
                    }
                }
                _selectedTipo.value = limpio
                closeDialog()
            } catch (e: Exception) {
                // Si ya alcanzamos a crear el tipo nuevo antes de que fallara la copia, se quita
                // para no dejar un cuestionario a medio duplicar en la lista (mismo rollback que
                // hace `duplicarCuestionario` en la web).
                if (tipoCreado) tipoRepository.deleteTipo(limpio)
            } finally {
                _tipoActionBusy.value = false
                tipoActionInFlight.set(false)
            }
        }
    }

    /** Junta las preguntas de todos los tipos en una sola lista, sin repetir concepto, para el picker "Elegir preguntas". */
    suspend fun cargarBancoPreguntas(): List<BancoPregunta> {
        val vistos = HashSet<String>()
        val resultado = mutableListOf<BancoPregunta>()
        for (t in tipos.value) {
            val secciones = catalogRepository.getAllSections(t).associateBy { it.id }
            for (p in catalogRepository.getAllQuestions(t)) {
                if (vistos.add(p.concepto.trim().lowercase())) {
                    resultado.add(BancoPregunta(p, t, secciones[p.seccionId]?.nombre.orEmpty()))
                }
            }
        }
        return resultado.sortedBy { it.question.concepto }
    }

    fun confirmAgregarDesdeBanco(seccionId: String, seleccionadas: List<QuestionEntity>) {
        if (seleccionadas.isEmpty()) return
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            for (p in seleccionadas) {
                catalogRepository.addPregunta(tipo, seccionId, p.concepto, p.credito, p.admiteFoto, p.descripcion, p.imagenEjemplo)
            }
            closeDialog()
        }
    }

    fun confirmRestaurarEjemplo() {
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            catalogRepository.restaurarEjemplo(tipo)
            closeDialog()
        }
    }

    fun confirmAddSection(icono: String, tituloLargo: String, tituloCorto: String) {
        if (tituloLargo.isBlank()) return
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            catalogRepository.addSeccion(tipo, icono, tituloLargo, tituloCorto)
            closeDialog()
        }
    }

    fun confirmEditSection(seccionId: String, icono: String, tituloLargo: String, tituloCorto: String) {
        if (tituloLargo.isBlank()) return
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            catalogRepository.updateSeccion(tipo, seccionId, icono, tituloLargo, tituloCorto)
            closeDialog()
        }
    }

    fun confirmDeleteSection(seccionId: String) {
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            catalogRepository.deleteSeccion(tipo, seccionId)
            closeDialog()
        }
    }

    fun confirmAddQuestion(seccionId: String, concepto: String, credito: Credito, admiteFoto: Boolean) {
        if (concepto.isBlank()) return
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            catalogRepository.addPregunta(tipo, seccionId, concepto, credito, admiteFoto)
            closeDialog()
        }
    }

    fun confirmEditQuestion(
        codigo: String,
        concepto: String,
        credito: Credito,
        admiteFoto: Boolean,
        descripcion: String,
        imagenEjemplo: String?
    ) {
        if (concepto.isBlank()) return
        val question = questions.value.find { it.codigo == codigo } ?: return
        viewModelScope.launch {
            catalogRepository.updateQuestion(
                question.copy(
                    concepto = concepto,
                    credito = credito,
                    admiteFoto = admiteFoto,
                    descripcion = descripcion,
                    imagenEjemplo = imagenEjemplo
                )
            )
            closeDialog()
        }
    }

    fun confirmDeleteQuestion(codigo: String) {
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            catalogRepository.deletePregunta(tipo, codigo)
            closeDialog()
        }
    }

    /** Persiste el nuevo orden de las secciones tras soltar el drag; ver [QuestionCatalogRepository.reorderSections]. */
    fun reorderSections(orderedIds: List<String>) {
        val tipo = _selectedTipo.value
        viewModelScope.launch { catalogRepository.reorderSections(tipo, orderedIds) }
    }

    /** Persiste el nuevo orden de las preguntas de una sección tras soltar el drag. */
    fun reorderQuestions(seccionId: String, orderedIds: List<String>) {
        val tipo = _selectedTipo.value
        viewModelScope.launch { catalogRepository.reorderQuestions(tipo, seccionId, orderedIds) }
    }
}
