package com.example.todoaccesible.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.dao.ProjectDao
import com.example.todoaccesible.data.local.entities.ProjectEntity
import com.example.todoaccesible.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Diagnostic(val id: String, val projectName: String, val clientName: String, val date: String)

class DashboardViewModel(private val projectDao: ProjectDao) : ViewModel() {
    private val _userRole = MutableStateFlow(UserRole.CLIENTE)
    val userRole: StateFlow<UserRole> = _userRole

    // Obtenemos los proyectos directamente de la base de datos como un Flow
    val projectsFlow = projectDao.getAllProjects()

    private val _pendingDiagnostics = MutableStateFlow(listOf(
        Diagnostic("1", "Plaza Comercial", "Juan Pérez", "12/03/2024"),
        Diagnostic("2", "Torre Médica", "Maria García", "13/03/2024")
    ))
    val pendingDiagnostics: StateFlow<List<Diagnostic>> = _pendingDiagnostics

    fun addProject(name: String, address: String, responsible: String, description: String) {
        viewModelScope.launch {
            val newProject = ProjectEntity(
                name = name,
                address = address,
                responsible = responsible,
                description = description,
                lastEvaluationDate = "Pendiente",
                compliancePercentage = 0
            )
            projectDao.insertProject(newProject)
        }
    }

    fun setRole(role: UserRole) {
        _userRole.value = role
    }
}
