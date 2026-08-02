package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.Credito

/** Pregunta del catálogo, codigo con formato "1.01", "3.24", etc. */
data class QuestionEntity(
    val codigo: String,
    val seccionId: String,
    val concepto: String,
    val credito: Credito,
    val admiteFoto: Boolean,
    val orden: Int,
    /** Ayuda opcional para el cliente (aclara el criterio a evaluar). */
    val descripcion: String = "",
    /** Las preguntas inactivas se excluyen del cuestionario del cliente y del scorecard, igual que una categoría inactiva. */
    val activa: Boolean = true,
    /** Uri (local, sin backend) de la imagen que el admin sube como guía de evaluación para el cliente. */
    val imagenReferenciaUri: String? = null
)
