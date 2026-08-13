package com.example.todoaccesible.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.data.model.Role
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthGroup(val clave: String, val etiqueta: String, val clientes: List<UserEntity>)

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private fun claveMes(createdAt: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = createdAt }
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
}

private fun etiquetaMes(createdAt: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = createdAt }
    return "${MESES[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

private fun agruparPorMes(clientes: List<UserEntity>): List<MonthGroup> {
    val ordenados = clientes.sortedByDescending { it.createdAt }
    val grupos = LinkedHashMap<String, MutableList<UserEntity>>()
    ordenados.forEach { cliente -> grupos.getOrPut(claveMes(cliente.createdAt)) { mutableListOf() }.add(cliente) }
    return grupos.map { (clave, lista) -> MonthGroup(clave, etiquetaMes(lista.first().createdAt), lista) }
}

/**
 * El alta de cuentas y el cambio de rol ya no están aquí: el backend no
 * expone ningún endpoint para eso (el alta real es el registro público, que
 * siempre crea rol `cliente`; el rol se fija según `ADMIN_EMAILS` del
 * servidor). Tampoco existe ya el control de "sesión activa forzar cierre"
 * (RF-18): era una simulación local sin equivalente en JWT sin estado. Y se
 * quitó "Comentar a un cliente": no hay endpoint para crear notificaciones
 * arbitrarias desde el panel admin (el backend solo las genera él mismo como
 * efecto secundario de aprobar/rechazar/etc), así que ese botón dejaba de
 * tener ningún efecto real contra el backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModel(
    private val userRepository: UserRepository,
    private val diagnosticRepository: DiagnosticRepository,
    private val tipoCuestionarioRepository: TipoCuestionarioRepository,
    private val toastController: ToastController
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> = userRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tipos: StateFlow<List<String>> = tipoCuestionarioRepository.observeTipos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Cupo de diagnósticos que puede iniciar un cliente; lo controla únicamente el administrador. `null` = ilimitados. */
    fun setDiagnosticosDisponibles(userId: Long, cantidad: Int?) {
        viewModelScope.launch { userRepository.setDiagnosticosDisponibles(userId, cantidad) }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch { userRepository.delete(userId) }
    }

    // ---- Búsqueda + agrupación por mes ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    fun setSearchQuery(value: String) { _searchQuery.value = value }

    private val _anioSeleccionado = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val anioSeleccionado: StateFlow<Int> = _anioSeleccionado
    fun cambiarAnioSeleccionado(delta: Int) { _anioSeleccionado.value += delta }

    private val _mesSeleccionado = MutableStateFlow<Int?>(null)
    val mesSeleccionado: StateFlow<Int?> = _mesSeleccionado

    private val _mostrarSelectorMeses = MutableStateFlow(false)
    val mostrarSelectorMeses: StateFlow<Boolean> = _mostrarSelectorMeses
    fun abrirSelectorMeses() { _mostrarSelectorMeses.value = true }
    fun cerrarSelectorMeses() { _mostrarSelectorMeses.value = false }
    fun seleccionarMes(mes: Int) { _mesSeleccionado.value = mes; _mostrarSelectorMeses.value = false }
    fun limpiarFiltroMes() { _mesSeleccionado.value = null; _mostrarSelectorMeses.value = false }

    private val _collapsedMonths = MutableStateFlow<Set<String>>(emptySet())
    val collapsedMonths: StateFlow<Set<String>> = _collapsedMonths
    fun toggleMonthCollapse(clave: String) {
        _collapsedMonths.value = _collapsedMonths.value.toMutableSet().apply { if (!add(clave)) remove(clave) }
    }

    private data class FiltroClientes(val query: String, val mes: Int?, val anio: Int)

    private val filtro = combine(_searchQuery, _mesSeleccionado, _anioSeleccionado) { q, m, a -> FiltroClientes(q, m, a) }

    val gruposPorMes: StateFlow<List<MonthGroup>> = combine(users, filtro) { list, f ->
        val clientes = list
            .filter { it.rol == Role.CLIENTE }
            .filter { f.query.isBlank() || it.nombre.contains(f.query, ignoreCase = true) || it.email.contains(f.query, ignoreCase = true) }
            .filter { f.mes == null || claveMes(it.createdAt) == "${f.anio}-${f.mes}" }
        agruparPorMes(clientes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Detalle de empresa ----

    private val _empresaTarget = MutableStateFlow<UserEntity?>(null)
    val empresaTarget: StateFlow<UserEntity?> = _empresaTarget

    /** Datos de empresa/inmueble capturados en el diagnóstico más antiguo del cliente (incluye borradores). */
    val empresaTargetDiagnostico: StateFlow<DiagnosticEntity?> = _empresaTarget.flatMapLatest { user ->
        if (user == null) flowOf(null) else diagnosticRepository.observeForCliente(user.id).map { it.minByOrNull(DiagnosticEntity::fechaCreacion) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun openEmpresaDetail(user: UserEntity) { _empresaTarget.value = user }
    fun closeEmpresaDetail() { _empresaTarget.value = null }

    // ---- Activar / desactivar cuenta ----

    private val _toggleTarget = MutableStateFlow<UserEntity?>(null)
    val toggleTarget: StateFlow<UserEntity?> = _toggleTarget
    fun requestDeactivate(user: UserEntity) { _toggleTarget.value = user }
    fun dismissDeactivate() { _toggleTarget.value = null }
    fun confirmDeactivate() {
        val user = _toggleTarget.value ?: return
        viewModelScope.launch {
            userRepository.setLicenseActive(user.id, false)
            _toggleTarget.value = null
            toastController.show("${user.nombre} desactivado", ToastTipo.ALERTA)
        }
    }

    private val _activarTarget = MutableStateFlow<UserEntity?>(null)
    val activarTarget: StateFlow<UserEntity?> = _activarTarget

    private val _tipoParaAsignar = MutableStateFlow("")
    val tipoParaAsignar: StateFlow<String> = _tipoParaAsignar
    fun onTipoParaAsignarChange(value: String) { _tipoParaAsignar.value = value }

    fun openActivarDialog(user: UserEntity) {
        _activarTarget.value = user
        _tipoParaAsignar.value = user.cuestionarioAsignado ?: tipos.value.firstOrNull().orEmpty()
    }

    fun dismissActivarDialog() { _activarTarget.value = null }

    fun confirmActivar() {
        val user = _activarTarget.value ?: return
        val tipo = _tipoParaAsignar.value
        viewModelScope.launch {
            userRepository.assignCuestionario(user.id, tipo)
            userRepository.setLicenseActive(user.id, true)
            _activarTarget.value = null
            toastController.show("${user.nombre} activado con cuestionario \"$tipo\"", ToastTipo.EXITO)
        }
    }
}
