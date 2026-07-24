package com.example.todoaccesible.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.seed.MexicoLocations
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
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * El registro público SIEMPRE crea un usuario con rol CLIENTE, en 2 pasos:
 * Paso 1 (cuenta) y Paso 2 (datos de la empresa/inmueble a evaluar). La
 * cuenta y el diagnóstico solo se crean al confirmar el Paso 2.
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
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }
        _uiState.value = state.copy(step = 2, error = null)
    }

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

    /** Crea la cuenta, guarda los datos de la empresa en el diagnóstico borrador y navega al cuestionario. */
    fun register(onSuccess: (diagnosticId: Long) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val result = authRepository.register(state.nombre, state.email, state.password)) {
                is AuthResult.Success -> {
                    val draft = diagnosticRepository.getOrCreateDraft(result.session.userId)
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
                        fechaEvaluacion = state.fechaEvaluacion
                    )
                    _uiState.value = _uiState.value.copy(loading = false)
                    onSuccess(draft.id)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
                // register() nunca produce conflicto de sesión (RF-18 solo aplica a login).
                is AuthResult.SessionConflict -> Unit
            }
        }
    }
}
