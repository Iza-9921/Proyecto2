package com.example.todoaccesible.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.seed.MexicoLocations
import com.example.todoaccesible.data.remote.ApiError
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class RegisterUiState(
    /** 1 = cuenta (correo/contraseña), 2 = datos de la empresa a evaluar. */
    val step: Int = 1,
    // Paso 1: cuenta
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    // Paso 2: empresa / proyecto a evaluar
    val projectName: String = "",
    val clienteNombre: String = "",
    val telefono: String = "",
    val ubicacion: String = "",
    val entidadFederativa: String = "",
    val ciudad: String = "",
    val tipoInmueble: String = "",
    val fechaEvaluacion: Long? = System.currentTimeMillis(),
    val responsable: String = "",
    val revision: String = "1",
    val logoEmpresaUri: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    /** Cuenta creada, pero el cliente no tiene cupo de diagnósticos asignado por el administrador. */
    val quotaBlocked: Boolean = false,
    /** Cuenta creada con éxito: se avisa que un admin debe activarla antes de poder usarla. */
    val showActivationNotice: Boolean = false,
    val pendingDiagnosticId: Long? = null
)

/**
 * El registro público SIEMPRE crea un usuario con rol CLIENTE, en 2 pasos:
 * Paso 1 (cuenta) y Paso 2 (datos de la empresa/inmueble a evaluar). La
 * cuenta y el diagnóstico solo se crean al confirmar el Paso 2.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val diagnosticRepository: DiagnosticRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    // ---- Paso 1: cuenta ----
    fun onNombreChange(value: String) { _uiState.value = _uiState.value.copy(nombre = value, error = null) }
    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value, error = null) }
    fun onPasswordChange(value: String) { _uiState.value = _uiState.value.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) { _uiState.value = _uiState.value.copy(confirmPassword = value, error = null) }

    fun goToStep2() {
        val state = _uiState.value
        if (state.nombre.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Completa todos los campos")
            return
        }
        if (!isPasswordSegura(state.password)) {
            _uiState.value = state.copy(
                error = "La contraseña debe tener al menos 8 caracteres, mayúscula, minúscula, número y un signo (ej. @, #, %)"
            )
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }
        _uiState.value = state.copy(step = 2, error = null)
    }

    private fun isPasswordSegura(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() } &&
            password.any { !it.isLetterOrDigit() }

    fun backToStep1() { _uiState.value = _uiState.value.copy(step = 1, error = null) }

    // ---- Paso 2: empresa / proyecto ----
    fun onProjectNameChange(value: String) { _uiState.value = _uiState.value.copy(projectName = value) }
    fun onClienteNombreChange(value: String) { _uiState.value = _uiState.value.copy(clienteNombre = value) }
    fun onTelefonoChange(value: String) { _uiState.value = _uiState.value.copy(telefono = value) }
    fun onUbicacionChange(value: String) { _uiState.value = _uiState.value.copy(ubicacion = value) }

    fun onEntidadFederativaChange(value: String) {
        val state = _uiState.value
        val ciudadSigueValida = MexicoLocations.ciudadesDe(value).contains(state.ciudad)
        _uiState.value = state.copy(entidadFederativa = value, ciudad = if (ciudadSigueValida) state.ciudad else "")
    }

    fun onCiudadChange(value: String) { _uiState.value = _uiState.value.copy(ciudad = value) }
    fun onTipoInmuebleChange(value: String) { _uiState.value = _uiState.value.copy(tipoInmueble = value) }
    fun onFechaEvaluacionChange(value: Long) { _uiState.value = _uiState.value.copy(fechaEvaluacion = value) }
    fun onResponsableChange(value: String) { _uiState.value = _uiState.value.copy(responsable = value) }
    fun onRevisionChange(value: String) { _uiState.value = _uiState.value.copy(revision = value) }
    fun onLogoEmpresaChange(value: String?) { _uiState.value = _uiState.value.copy(logoEmpresaUri = value) }

    /** Crea la cuenta y guarda los datos de la empresa en el diagnóstico borrador; el paso al cuestionario espera a que el usuario cierre el aviso de activación. */
    fun register() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val result = authRepository.register(state.nombre, state.email, state.password)) {
                is AuthResult.Success -> {
                    val draft = diagnosticRepository.getOrCreateDraft(result.session.userId)
                    try {
                        diagnosticRepository.updateProjectInfo(
                            diagnosticId = draft.id,
                            projectName = state.projectName,
                            ubicacion = state.ubicacion,
                            responsable = state.responsable,
                            revision = state.revision,
                            clienteNombre = state.clienteNombre,
                            telefono = state.telefono,
                            entidadFederativa = state.entidadFederativa,
                            ciudad = state.ciudad,
                            tipoInmueble = state.tipoInmueble,
                            fechaEvaluacion = state.fechaEvaluacion,
                            logoEmpresaUri = state.logoEmpresaUri
                        )
                    } catch (e: Exception) {
                        // La cuenta ya se creó (register tuvo éxito); solo falló crear el proyecto/diagnóstico
                        // en el backend (p.ej. 403 licencia vencida/inactiva). Se deja el mensaje visible y el
                        // borrador se completa más tarde desde "Nuevo diagnóstico" en el dashboard.
                        val mapped = ApiErrorMapper.from(e)
                        val message = if (mapped is ApiError.LicenciaVencida) {
                            "Tu cuenta se creó, pero tu licencia está vencida o inactiva: contacta al administrador para poder iniciar un diagnóstico."
                        } else {
                            mapped.message
                        }
                        _uiState.value = _uiState.value.copy(loading = false, error = message)
                        return@launch
                    }
                    val disponibles = userRepository.observeById(result.session.userId).firstOrNull()?.diagnosticosDisponibles
                    if (disponibles != null && disponibles <= 0) {
                        _uiState.value = _uiState.value.copy(loading = false, quotaBlocked = true)
                    } else {
                        _uiState.value = _uiState.value.copy(loading = false, showActivationNotice = true, pendingDiagnosticId = draft.id)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun dismissQuotaBlocked() { _uiState.value = _uiState.value.copy(quotaBlocked = false) }

    fun acknowledgeActivationNotice(onSuccess: (diagnosticId: Long) -> Unit) {
        val diagnosticId = _uiState.value.pendingDiagnosticId ?: return
        _uiState.value = _uiState.value.copy(showActivationNotice = false, pendingDiagnosticId = null)
        onSuccess(diagnosticId)
    }
}
