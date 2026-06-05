package com.example.todoaccesible.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.todoaccesible.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val responsible: String,
    val description: String = "",
    val status: String = "Pendiente",
    val lastEvaluationDate: String = "Sin evaluar",
    val compliancePercentage: Int = 0
)

data class Diagnostic(val id: String, val projectName: String, val clientName: String, val date: String)

class DashboardViewModel : ViewModel() {
    private val _userRole = MutableStateFlow(UserRole.CLIENTE)
    val userRole: StateFlow<UserRole> = _userRole

    private val _projects = MutableStateFlow(listOf(
        Project(
            name = "Corporativo CDMX",
            address = "Av. Reforma 123",
            responsible = "Ana López",
            status = "Excelente",
            lastEvaluationDate = "10/02/2024",
            compliancePercentage = 92
        ),
        Project(
            name = "Sucursal Cuernavaca",
            address = "Centro Histórico",
            responsible = "Carlos Ruiz",
            status = "Regular",
            lastEvaluationDate = "15/01/2024",
            compliancePercentage = 78
        )
    ))
    val projects: StateFlow<List<Project>> = _projects

    private val _pendingDiagnostics = MutableStateFlow(listOf(
        Diagnostic("1", "Plaza Comercial", "Juan Pérez", "12/03/2024"),
        Diagnostic("2", "Torre Médica", "Maria García", "13/03/2024")
    ))
    val pendingDiagnostics: StateFlow<List<Diagnostic>> = _pendingDiagnostics

    fun addProject(name: String, address: String, responsible: String, description: String) {
        val newProject = Project(
            name = name,
            address = address,
            responsible = responsible,
            description = description
        )
        _projects.value = _projects.value + newProject
    }

    fun setRole(role: UserRole) {
        _userRole.value = role
    }
}
