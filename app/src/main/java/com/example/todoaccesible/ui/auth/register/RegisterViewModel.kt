package com.example.todoaccesible.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

/** El registro público SIEMPRE crea un usuario con rol CLIENTE. */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onNombreChange(value: String) { _uiState.value = _uiState.value.copy(nombre = value, error = null) }
    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value, error = null) }
    fun onPasswordChange(value: String) { _uiState.value = _uiState.value.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) { _uiState.value = _uiState.value.copy(confirmPassword = value, error = null) }

    fun register(onSuccess: () -> Unit) {
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
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val result = authRepository.register(state.nombre, state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false)
                    onSuccess()
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
