package com.example.todoaccesible.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.model.UserRole
import com.example.todoaccesible.data.preferences.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _selectedRole = MutableStateFlow(UserRole.CLIENTE)
    val selectedRole: StateFlow<UserRole> = _selectedRole

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onRoleChange(newRole: UserRole) {
        _selectedRole.value = newRole
    }

    fun login(onSuccess: (UserRole) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Simulación de guardado de token y rol
            val dummyToken = "dummy_jwt_token"
            val role = _selectedRole.value
            
            tokenManager.saveToken(dummyToken)
            tokenManager.saveRole(role)
            
            _isLoading.value = false
            onSuccess(role)
        }
    }
}
