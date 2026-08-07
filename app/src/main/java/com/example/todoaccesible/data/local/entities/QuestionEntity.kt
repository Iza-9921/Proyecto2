package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.Credito

/**
 * Pregunta del catálogo, codigo con formato "1.01", "3.24", etc., único
 * solo dentro de su [tipo] de inmueble (cada tipo tiene su propio catálogo
 * independiente). [descripcion] e [imagenEjemplo] son el contenido
 * instructivo opcional ("Ver cómo hacerlo") que el admin puede capturar
 * por pregunta.
 */
data class QuestionEntity(
    val codigo: String,
    val tipo: String,
    val seccionId: String,
    val concepto: String,
    val credito: Credito,
    val admiteFoto: Boolean,
    val orden: Int,
    val descripcion: String = "",
    val imagenEjemplo: String? = null
)
