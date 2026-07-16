package com.example.todoaccesible.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewUserForm(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val rol: Role = Role.CLIENTE
)

class UserManagementViewModel(private val userRepository: UserRepository) : ViewModel() {

    val users: StateFlow<List<UserEntity>> = userRepository.observeAll()
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

    fun deleteUser(userId: Long) {
        viewModelScope.launch { userRepository.delete(userId) }
    }
}
