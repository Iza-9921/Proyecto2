package com.example.todoaccesible.ui.newproject

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NewProjectViewModel : ViewModel() {
    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city

    private val _propertyType = MutableStateFlow("")
    val propertyType: StateFlow<String> = _propertyType

    private val _contactName = MutableStateFlow("")
    val contactName: StateFlow<String> = _contactName

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    private val _observations = MutableStateFlow("")
    val observations: StateFlow<String> = _observations

    fun onProjectNameChange(newValue: String) { _projectName.value = newValue }
    fun onAddressChange(newValue: String) { _address.value = newValue }
    fun onCityChange(newValue: String) { _city.value = newValue }
    fun onPropertyTypeChange(newValue: String) { _propertyType.value = newValue }
    fun onContactNameChange(newValue: String) { _contactName.value = newValue }
    fun onPhoneChange(newValue: String) { _phone.value = newValue }
    fun onObservationsChange(newValue: String) { _observations.value = newValue }

    fun startEvaluation(onSuccess: () -> Unit) {
        // Lógica para guardar el proyecto y pasar a la evaluación
        onSuccess()
    }
}
