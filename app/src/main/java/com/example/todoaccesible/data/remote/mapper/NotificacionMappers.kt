package com.example.todoaccesible.data.remote.mapper

import com.example.todoaccesible.data.local.entities.NotificationEntity
import com.example.todoaccesible.data.remote.dto.NotificacionDto

fun NotificacionDto.toEntity(fallbackUserId: Long): NotificationEntity = NotificationEntity(
    id = id,
    diagnosticId = diagnosticoId,
    destinatarioId = destinatarioId ?: fallbackUserId,
    tipo = evento ?: tipo ?: categoria ?: "notificacion",
    mensaje = cuerpo ?: titulo.orEmpty(),
    leido = leida,
    fecha = parseBackendDate(fecha)
)
