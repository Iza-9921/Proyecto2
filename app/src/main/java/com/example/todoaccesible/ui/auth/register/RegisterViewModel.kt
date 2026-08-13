package com.example.todoaccesible.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.seed.MexicoLocations
import com.example.todoaccesible.data.remote.ApiError
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import com.example.todoaccesible.data.repository.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    /** Cuenta creada con éxito, pero bloqueada hasta que un admin la active. Al confirmar, se cierra la sesión recién creada. */
    val showActivationNotice: Boolean = false
)

/**
 * El registro público SIEMPRE crea un usuario con rol CLIENTE, en 2 pasos:
 * Paso 1 (cuenta) y Paso 2 (datos de la empresa/inmueble a evaluar). La
 * cuenta y el diagnóstico solo se crean al confirmar el Paso 2. La cuenta
 * queda inactiva hasta que un admin la habilita (`activo = false` en el
 * backend para todo registro que no sea de un correo admin), así que no se
 * deja al usuario entrar a la app con la sesión recién creada: se avisa y se
 * cierra esa sesión, obligando a iniciar sesión de nuevo una vez activada.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val diagnosticRepository: DiagnosticRepository
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

    /** Crea la cuenta y guarda los datos de la empresa en el diagnóstico borrador (queda como borrador, resumible tras iniciar sesión ya activa); no navega a ningún lado, solo muestra el aviso de cuenta bloqueada. */
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
                    _uiState.value = _uiState.value.copy(loading = false, showActivationNotice = true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    /** El usuario confirmó el aviso: se cierra la sesión (la cuenta sigue bloqueada) y se regresa a Login. */
    fun acknowledgeActivationNotice(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(showActivationNotice = false)
            onDone()
        }
    }
}
