package com.example.todoaccesible.data.remote.dto

data class NotificacionDto(
    val id: Long,
    val destinatarioId: Long? = null,
    val categoria: String? = null,
    val titulo: String? = null,
    val cuerpo: String? = null,
    val fecha: String? = null,
    val leida: Boolean = false,
    val diagnosticoId: Long? = null,
    val evento: String? = null,
    val tipo: String? = null,
    val autor: String? = null
)

data class NotificacionesResponseDto(
    val notificaciones: List<NotificacionDto> = emptyList(),
    val total: Int = 0,
    val no_leidas: Int = 0,
    val page: Int = 1,
    val limit: Int = 10
)
