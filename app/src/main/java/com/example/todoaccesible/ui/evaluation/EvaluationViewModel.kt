package com.example.todoaccesible.ui.evaluation

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.todoaccesible.data.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EvaluationViewModel : ViewModel() {

    private val _questions = MutableStateFlow(listOf(
        Question(1, "¿El acceso principal cuenta con rampa?"),
        Question(2, "¿Los pasillos tienen un ancho mínimo de 1.20m?"),
        Question(3, "¿Existen baños adaptados para personas con discapacidad?")
    ))
    val questions: StateFlow<List<Question>> = _questions

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    fun onAnswerChange(index: Int, answer: String) {
        val updatedList = _questions.value.toMutableList()
        updatedList[index] = updatedList[index].copy(answer = answer)
        _questions.value = updatedList
    }

    fun onPhotoCaptured(index: Int, uri: Uri) {
        val updatedList = _questions.value.toMutableList()
        updatedList[index] = updatedList[index].copy(photoUri = uri)
        _questions.value = updatedList
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            _currentQuestionIndex.value++
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
        }
    }
}
