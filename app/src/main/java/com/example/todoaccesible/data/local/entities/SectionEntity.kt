package com.example.todoaccesible.data.local.entities

/** Categoría del cuestionario (las 8 originales del Scorecard v2.6 más las que cree el admin). */
data class SectionEntity(
    val id: String,
    val nombre: String,
    val orden: Int,
    /** Las categorías inactivas se excluyen del cuestionario que responde el cliente y del scorecard, pero se conservan para no perder el historial ya capturado. */
    val activa: Boolean = true
)
