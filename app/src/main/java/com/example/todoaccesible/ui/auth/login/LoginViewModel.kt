package com.example.todoaccesible.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun login(onSuccess: (Role) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Ingresa tu correo y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val result = authRepository.login(state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false)
                    onSuccess(result.session.rol)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
