package com.example.todoaccesible.ui.auth.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.PasswordResetResult
import com.example.todoaccesible.data.repository.VerifyResetCodeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ForgotPasswordStep { EMAIL, CODE, NEW_PASSWORD, DONE }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.EMAIL,
    val email: String = "",
    val codigo: String = "",
    val nuevaContrasena: String = "",
    val confirmarContrasena: String = "",
    val resetToken: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
)

/**
 * Flujo "olvidé mi contraseña" en 3 pasos, contra los endpoints reales del
 * backend: `auth/recuperar` (envía código de 6 dígitos al correo),
 * `auth/verificar-codigo` (canjea el código por un `reset_token` de 10 min) y
 * `auth/nueva-contrasena` (fija la contraseña usando ese token).
 */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value, error = null) }
    fun onCodigoChange(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(codigo = value, error = null)
        }
    }
    fun onNuevaContrasenaChange(value: String) { _uiState.value = _uiState.value.copy(nuevaContrasena = value, error = null) }
    fun onConfirmarContrasenaChange(value: String) { _uiState.value = _uiState.value.copy(confirmarContrasena = value, error = null) }

    /** Paso 1: pide el código. El backend siempre responde "éxito" (no revela si el correo existe). */
    fun solicitarCodigo() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresa tu correo electrónico")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = authRepository.requestPasswordReset(email)) {
                is PasswordResetResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        step = ForgotPasswordStep.CODE,
                        infoMessage = "Si el correo existe, se envió un código de 6 dígitos. Revisa tu bandeja de entrada."
                    )
                }
                is PasswordResetResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    /** Paso 2: canjea el código por el `reset_token`. */
    fun verificarCodigo() {
        val state = _uiState.value
        if (state.codigo.length != 6) {
            _uiState.value = state.copy(error = "El código debe tener 6 dígitos")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = authRepository.verifyResetCode(state.email.trim(), state.codigo)) {
                is VerifyResetCodeResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        step = ForgotPasswordStep.NEW_PASSWORD,
                        resetToken = result.resetToken,
                        infoMessage = null
                    )
                }
                is VerifyResetCodeResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun reenviarCodigo() = solicitarCodigo()

    fun volverAPedirCodigo() {
        _uiState.value = _uiState.value.copy(step = ForgotPasswordStep.EMAIL, codigo = "", error = null, infoMessage = null)
    }

    /** Paso 3: fija la nueva contraseña con el `reset_token` obtenido en el paso anterior. */
    fun establecerNuevaContrasena() {
        val state = _uiState.value
        val resetToken = state.resetToken
        if (resetToken == null) {
            _uiState.value = state.copy(error = "El código expiró, vuelve a solicitarlo", step = ForgotPasswordStep.EMAIL)
            return
        }
        if (!isPasswordSegura(state.nuevaContrasena)) {
            _uiState.value = state.copy(
                error = "La contraseña debe tener al menos 8 caracteres, mayúscula, minúscula, número y un signo (ej. @, #, %)"
            )
            return
        }
        if (state.nuevaContrasena != state.confirmarContrasena) {
            _uiState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = authRepository.setNewPassword(resetToken, state.nuevaContrasena)) {
                is PasswordResetResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false, step = ForgotPasswordStep.DONE)
                }
                is PasswordResetResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    private fun isPasswordSegura(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() } &&
            password.any { !it.isLetterOrDigit() }
}
