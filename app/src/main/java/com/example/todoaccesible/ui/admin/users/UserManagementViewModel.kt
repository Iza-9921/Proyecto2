package com.example.todoaccesible.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.ActiveSessionRegistry
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import com.example.todoaccesible.data.repository.UserRepository
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

data class NewUserForm(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val rol: Role = Role.CLIENTE
)

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

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModel(
    private val userRepository: UserRepository,
    private val activeSessionRegistry: ActiveSessionRegistry,
    private val diagnosticRepository: DiagnosticRepository,
    private val tipoCuestionarioRepository: TipoCuestionarioRepository,
    private val notificationRepository: NotificationRepository,
    private val toastController: ToastController
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> = userRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** RF-18: qué usuarios tienen una sesión activa ahora mismo, para que el admin las supervise. */
    val activeSessionUserIds: StateFlow<Set<Long>> = activeSessionRegistry.observeActiveUserIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val tipos: StateFlow<List<String>> = tipoCuestionarioRepository.observeTipos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(NewUserForm())
    val form: StateFlow<NewUserForm> = _form

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun openCreateDialog() { _form.value = NewUserForm(); _error.value = null; _showCreateDialog.value = true }
    fun dismissCreateDialog() { _showCreateDialog.value = false }

    fun onNombreChange(value: String) { _form.value = _form.value.copy(nombre = value) }
    fun onEmailChange(value: String) { _form.value = _form.value.copy(email = value) }
    fun onPasswordChange(value: String) { _form.value = _form.value.copy(password = value) }
    fun onRoleChange(value: Role) { _form.value = _form.value.copy(rol = value) }

    fun createUser() {
        val f = _form.value
        if (f.nombre.isBlank() || f.email.isBlank() || f.password.length < 6) {
            _error.value = "Completa nombre, correo y una contraseña de al menos 6 caracteres"
            return
        }
        viewModelScope.launch {
            val result = userRepository.create(f.nombre, f.email, f.password, f.rol)
            result.onSuccess { _showCreateDialog.value = false }
                .onFailure { _error.value = it.message }
        }
    }

    fun updateRole(userId: Long, rol: Role) {
        viewModelScope.launch { userRepository.updateRole(userId, rol) }
    }

    /** Cupo de diagnósticos que puede iniciar un cliente; lo controla únicamente el administrador. `null` = ilimitados. */
    fun setDiagnosticosDisponibles(userId: Long, cantidad: Int?) {
        viewModelScope.launch { userRepository.setDiagnosticosDisponibles(userId, cantidad) }
    }

    /** RF-18: el admin fuerza el cierre de una sesión activa (p.ej. si quedó colgada). */
    fun forceCloseSession(userId: Long) {
        activeSessionRegistry.markInactive(userId)
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

    // ---- Comentario a un cliente ----

    private val _comentarioTarget = MutableStateFlow<UserEntity?>(null)
    val comentarioTarget: StateFlow<UserEntity?> = _comentarioTarget

    val comentarioTargetDiagnosticos: StateFlow<List<DiagnosticEntity>> = _comentarioTarget.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else diagnosticRepository.observeForCliente(user.id)
            .map { list -> list.filter { it.estado != DiagnosticStatus.BORRADOR } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openComentarioDialog(user: UserEntity) { _comentarioTarget.value = user }
    fun closeComentarioDialog() { _comentarioTarget.value = null }

    fun enviarComentario(categoria: String, mensaje: String, diagnosticoId: Long?) {
        val destinatario = _comentarioTarget.value ?: return
        if (mensaje.isBlank()) return
        viewModelScope.launch {
            notificationRepository.notify(
                diagnosticId = diagnosticoId,
                destinatarioId = destinatario.id,
                tipo = categoria,
                mensaje = mensaje
            )
            _comentarioTarget.value = null
            toastController.show("Comentario enviado", ToastTipo.EXITO)
        }
    }

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
