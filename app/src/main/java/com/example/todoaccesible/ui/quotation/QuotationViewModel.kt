package com.example.todoaccesible.ui.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.dao.QuotationDao
import com.example.todoaccesible.data.local.entities.QuotationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QuotationViewModel(private val quotationDao: QuotationDao) : ViewModel() {

    private val _companyName = MutableStateFlow("")
    val companyName: StateFlow<String> = _companyName

    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName

    private val _contactName = MutableStateFlow("")
    val contactName: StateFlow<String> = _contactName

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    private val _comments = MutableStateFlow("")
    val comments: StateFlow<String> = _comments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun onCompanyNameChange(value: String) { _companyName.value = value }
    fun onProjectNameChange(value: String) { _projectName.value = value }
    fun onContactNameChange(value: String) { _contactName.value = value }
    fun onEmailChange(value: String) { _email.value = value }
    fun onPhoneChange(value: String) { _phone.value = value }
    fun onCommentsChange(value: String) { _comments.value = value }

    fun sendQuotation(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val quotation = QuotationEntity(
                companyName = _companyName.value,
                projectName = _projectName.value,
                contactName = _contactName.value,
                email = _email.value,
                phone = _phone.value,
                comments = _comments.value,
                requestDate = date
            )
            quotationDao.insertQuotation(quotation)
            // Aquí se simularía el envío de correo
            _isLoading.value = false
            onSuccess()
        }
    }
}
