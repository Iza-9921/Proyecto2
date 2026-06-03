package com.example.todoaccesible.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.todoaccesible.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Project(val id: String, val name: String, val status: String)
data class Diagnostic(val id: String, val projectName: String, val clientName: String, val date: String)

class DashboardViewModel : ViewModel() {
    // Simulamos el rol del usuario. En una app real vendría del login/token.
    private val _userRole = MutableStateFlow(UserRole.CLIENTE)
    val userRole: StateFlow<UserRole> = _userRole

    private val _projects = MutableStateFlow(listOf(
        Project("1", "Edificio Central", "En progreso"),
        Project("2", "Sucursal Norte", "Completado")
    ))
    val projects: StateFlow<List<Project>> = _projects

    private val _pendingDiagnostics = MutableStateFlow(listOf(
        Diagnostic("1", "Plaza Comercial", "Juan Pérez", "12/03/2024"),
        Diagnostic("2", "Torre Médica", "Maria García", "13/03/2024")
    ))
    val pendingDiagnostics: StateFlow<List<Diagnostic>> = _pendingDiagnostics

    fun setRole(role: UserRole) {
        _userRole.value = role
    }
}
