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

data class QuestionCatalogUiState(
    val tipos: List<String> = emptyList(),
    val selectedTipo: String = "",
    val sections: List<SectionEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val expandedSections: Set<String> = emptySet(),
    val dialog: QuestionCatalogDialog? = null
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

    private val tipos: StateFlow<List<String>> = tipoRepository.observeTipos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sections: StateFlow<List<SectionEntity>> = _selectedTipo.flatMapLatest { tipo ->
        if (tipo.isBlank()) flowOf(emptyList()) else catalogRepository.observeSections(tipo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val questions: StateFlow<List<QuestionEntity>> = _selectedTipo.flatMapLatest { tipo ->
        if (tipo.isBlank()) flowOf(emptyList()) else catalogRepository.observeQuestions(tipo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class BaseState(val tipos: List<String>, val selectedTipo: String, val sections: List<SectionEntity>)
    private data class ExtraState(val questions: List<QuestionEntity>, val expanded: Set<String>, val dialog: QuestionCatalogDialog?)

    private val base = combine(tipos, _selectedTipo, sections) { t, s, sec -> BaseState(t, s, sec) }
    private val extra = combine(questions, _expandedSections, _dialog) { q, e, d -> ExtraState(q, e, d) }

    val uiState: StateFlow<QuestionCatalogUiState> = combine(base, extra) { b, e ->
        QuestionCatalogUiState(b.tipos, b.selectedTipo, b.sections, e.questions, e.expanded, e.dialog)
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

    fun confirmCreateTipo(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            tipoRepository.addTipo(limpio)
            _selectedTipo.value = limpio
            closeDialog()
        }
    }

    fun confirmDeleteTipo() {
        val tipo = _selectedTipo.value
        viewModelScope.launch {
            tipoRepository.deleteTipo(tipo)
            _selectedTipo.value = tipos.value.firstOrNull { it != tipo }.orEmpty()
            closeDialog()
        }
    }

    fun confirmRenombrarTipo(nombreNuevo: String) {
        val limpio = nombreNuevo.trim()
        if (limpio.isEmpty()) return
        val actual = _selectedTipo.value
        viewModelScope.launch {
            tipoRepository.renombrarTipo(actual, limpio)
            _selectedTipo.value = limpio
            closeDialog()
        }
    }

    fun confirmDuplicarTipo(nombreNuevo: String) {
        val limpio = nombreNuevo.trim()
        if (limpio.isEmpty()) return
        val tipoOrigen = _selectedTipo.value
        viewModelScope.launch {
            tipoRepository.addTipo(limpio)
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
                    catalogRepository.addPregunta(
                        limpio, nuevaSeccion.id, p.concepto, p.credito, p.admiteFoto, p.descripcion, p.imagenEjemplo
                    )
                }
            }
            _selectedTipo.value = limpio
            closeDialog()
        }
    }

    /** Junta las preguntas de todos los tipos en una sola lista, sin repetir concepto, para el picker "Elegir preguntas". */
    suspend fun cargarBancoPreguntas(): List<QuestionEntity> {
        val vistos = HashSet<String>()
        val resultado = mutableListOf<QuestionEntity>()
        for (t in tipos.value) {
            for (p in catalogRepository.getAllQuestions(t)) {
                if (vistos.add(p.concepto.trim().lowercase())) resultado.add(p)
            }
        }
        return resultado.sortedBy { it.concepto }
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
}
