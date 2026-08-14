package com.example.todoaccesible.data.remote.dto

data class ProyectoRequest(
    val nombre_proyecto: String,
    val cliente: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val estado: String? = null,
    val tipo_inmueble: String? = null,
    val fecha_evaluacion: String? = null
)

data class ProyectoDto(
    val id: Long,
    val usuario_id: Long,
    val nombre_proyecto: String,
    val cliente: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val estado: String? = null,
    val tipo_inmueble: String? = null,
    val fecha_evaluacion: String? = null,
    val activo: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)
