package com.example.todoaccesible.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ForgotPasswordStep {
    EMAIL, CODE_AND_PASSWORD
}

class ForgotPasswordViewModel : ViewModel() {

    private val _step = MutableStateFlow(ForgotPasswordStep.EMAIL)
    val step: StateFlow<ForgotPasswordStep> = _step

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onCodeChange(newCode: String) {
        if (newCode.length <= 6) {
            _code.value = newCode
        }
    }

    fun onNewPasswordChange(newPassword: String) {
        _newPassword.value = newPassword
    }

    fun sendCode() {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulación de envío de código vía Retrofit
            delay(1500) 
            _isLoading.value = false
            _step.value = ForgotPasswordStep.CODE_AND_PASSWORD
        }
    }

    fun resetPassword(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulación de cambio de contraseña vía Retrofit
            delay(1500)
            _isLoading.value = false
            onSuccess()
        }
    }
}
