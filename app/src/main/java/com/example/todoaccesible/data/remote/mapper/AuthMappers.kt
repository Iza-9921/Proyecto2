package com.example.todoaccesible.data.remote.mapper

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.remote.dto.AdminUsuarioDto
import com.example.todoaccesible.data.remote.dto.AuthUserDto

/** El usuario autenticado tal como viene de login/register/refresh (no hay endpoint "me"). */
fun AuthUserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    passwordHash = "",
    nombre = name,
    rol = role.toRole(),
    licenseActive = activo,
    diagnosticosDisponibles = limiteCuestionarios,
    createdAt = System.currentTimeMillis(),
    cuestionarioAsignado = cuestionarioAsignado
)

/** `GET /admin/usuarios` — ojo: `id` viene como String, hay que convertirlo. */
fun AdminUsuarioDto.toEntity(): UserEntity = UserEntity(
    id = id.toLongOrNull() ?: 0L,
    email = email,
    passwordHash = "",
    nombre = name,
    rol = role.toRole(),
    licenseActive = activo,
    diagnosticosDisponibles = limiteCuestionarios,
    createdAt = parseBackendDate(createdAt),
    cuestionarioAsignado = cuestionarioAsignado
)
