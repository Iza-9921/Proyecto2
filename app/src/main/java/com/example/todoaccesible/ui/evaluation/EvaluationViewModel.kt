package com.example.todoaccesible.ui.evaluation

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.todoaccesible.data.local.dao.AnswerDao
import com.example.todoaccesible.data.model.AnswerType
import com.example.todoaccesible.data.model.EvaluationResult
import com.example.todoaccesible.data.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EvaluationViewModel(private val answerDao: AnswerDao) : ViewModel() {

    private val _questions = MutableStateFlow(listOf(
        // Accesibilidad Exterior
        Question(1, "Accesibilidad Exterior", "¿Una persona en silla de ruedas puede llegar desde la calle hasta la entrada principal sin obstáculos?"),
        Question(2, "Accesibilidad Exterior", "¿Existe un área segura para ascenso y descenso de pasajeros cerca del acceso principal?"),
        Question(3, "Accesibilidad Exterior", "¿Los recorridos peatonales exteriores son amplios y están libres de obstáculos?"),
        Question(4, "Accesibilidad Exterior", "¿Los peatones cuentan con protección frente a la circulación de vehículos?"),
        Question(5, "Accesibilidad Exterior", "¿Los cruces peatonales son seguros y fáciles de utilizar?"),
        Question(6, "Accesibilidad Exterior", "¿Existen elementos en el piso que ayuden a orientarse a personas con discapacidad visual?"),
        Question(7, "Accesibilidad Exterior", "¿Existe un mapa o directorio para ayudar a los usuarios a ubicarse?"),
        
        // Ruta Accesible Interior
        Question(8, "Ruta Accesible Interior", "¿Las principales áreas del inmueble están conectadas mediante recorridos accesibles?"),
        Question(9, "Ruta Accesible Interior", "¿Los pasillos permiten el paso cómodo de una silla de ruedas?"),
        Question(10, "Ruta Accesible Interior", "¿Los pasillos están libres de obstáculos?"),
        Question(11, "Ruta Accesible Interior", "¿Los pisos son seguros y antideslizantes?"),
        Question(12, "Ruta Accesible Interior", "¿Existen ayudas para orientar a personas con discapacidad visual?"),
        Question(13, "Ruta Accesible Interior", "¿Las áreas de uso común cuentan con señalización fácil de identificar?"),

        // Elementos de la Ruta Accesible
        Question(14, "Elementos de la Ruta Accesible", "¿Las puertas permiten el paso cómodo de una silla de ruedas?"),
        Question(15, "Elementos de la Ruta Accesible", "¿Existe espacio suficiente para maniobrar frente a las puertas?"),
        Question(16, "Elementos de la Ruta Accesible", "¿Las puertas son fáciles de abrir?"),
        Question(17, "Elementos de la Ruta Accesible", "¿Las puertas o muros de vidrio están señalizados para evitar accidentes?"),
        Question(18, "Elementos de la Ruta Accesible", "¿Los accesos controlados permiten el paso de personas con discapacidad?"),
        Question(19, "Elementos de la Ruta Accesible", "¿Los mostradores de atención pueden ser utilizados por una persona en silla de ruedas?"),
        Question(20, "Elementos de la Ruta Accesible", "¿Existen rampas donde hay cambios de nivel?"),
        Question(21, "Elementos de la Ruta Accesible", "¿Las rampas son seguras y cómodas de utilizar?"),
        Question(22, "Elementos de la Ruta Accesible", "¿Las escaleras cuentan con pasamanos?"),
        Question(23, "Elementos de la Ruta Accesible", "¿Existe elevador o plataforma para acceder a todos los niveles?"),
        Question(24, "Elementos de la Ruta Accesible", "¿El elevador puede ser utilizado cómodamente por una persona en silla de ruedas?"),
        Question(25, "Elementos de la Ruta Accesible", "¿Los controles del elevador son fáciles de alcanzar?"),
        Question(26, "Elementos de la Ruta Accesible", "¿El elevador proporciona información sonora sobre su funcionamiento?"),

        // Servicios Sanitarios
        Question(27, "Servicios Sanitarios", "¿Los sanitarios son fáciles de localizar y acceder?"),
        Question(28, "Servicios Sanitarios", "¿Existe al menos un sanitario accesible?"),
        Question(29, "Servicios Sanitarios", "¿Existe espacio suficiente para maniobrar dentro del sanitario accesible?"),
        Question(30, "Servicios Sanitarios", "¿Los lavamanos pueden utilizarse desde una silla de ruedas?"),
        Question(31, "Servicios Sanitarios", "¿Los excusados accesibles cuentan con barras de apoyo?"),
        Question(32, "Servicios Sanitarios", "¿Existe una alarma visual dentro de los sanitarios?"),

        // Recursos Humanos y Administración
        Question(33, "Recursos Humanos y Administración", "¿Existe una política de no discriminación laboral?"),
        Question(34, "Recursos Humanos y Administración", "¿Se permite el acceso de perros de asistencia?"),

        // Protección Civil
        Question(35, "Protección Civil", "¿La señalización de emergencia es visible y fácil de identificar?"),
        Question(36, "Protección Civil", "¿Las salidas de emergencia son accesibles?"),
        Question(37, "Protección Civil", "¿Las alarmas pueden ser percibidas por personas con discapacidad visual y auditiva?"),
        Question(38, "Protección Civil", "¿Existe un área segura de resguardo para personas con discapacidad?"),
        Question(39, "Protección Civil", "¿Existe un procedimiento de evacuación para personas con discapacidad?"),
        Question(40, "Protección Civil", "¿Los extintores son visibles y fáciles de alcanzar?")
    ))
    val questions: StateFlow<List<Question>> = _questions

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    fun onAnswerChange(index: Int, answer: AnswerType) {
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

    fun calculateResult(): EvaluationResult {
        val answeredQuestions = _questions.value.filter { it.answer != null && it.answer != AnswerType.NA }
        
        val sumPuntos = answeredQuestions.sumOf { it.answer?.points ?: 0 }
        val totalPosible = answeredQuestions.size * 100
        
        val percentage = if (totalPosible > 0) (sumPuntos * 100) / totalPosible else 0
        
        return EvaluationResult(
            percentage = percentage,
            level = EvaluationResult.classify(percentage),
            countSi = _questions.value.count { it.answer == AnswerType.SI },
            countParcialmente = _questions.value.count { it.answer == AnswerType.PARCIALMENTE },
            countNo = _questions.value.count { it.answer == AnswerType.NO },
            countNA = _questions.value.count { it.answer == AnswerType.NA },
            totalQuestions = _questions.value.size
        )
    }
}
