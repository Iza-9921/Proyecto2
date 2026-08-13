package com.example.todoaccesible.data.remote.mapper

import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.remote.dto.AdminDiagnosticoRowDto
import com.example.todoaccesible.data.remote.dto.DiagnosticoPendienteDto

/**
 * Reconstrucción "ligera" de un [DiagnosticEntity] a partir de un listado
 * admin (`GET /admin/diagnosticos`), que NO trae todos los campos del
 * diagnóstico completo (ubicación, responsable, teléfono, nivel exacto...).
 * Se usa solo para las pantallas de listado (AdminPending/AdminDashboard);
 * el detalle real siempre se recarga completo vía `GET /diagnosticos/:id`.
 */
fun AdminDiagnosticoRowDto.toLightEntity(): DiagnosticEntity = DiagnosticEntity(
    id = diagnostico_id,
    clienteId = usuario_id ?: 0L,
    projectName = proyecto_nombre.orEmpty(),
    ubicacion = "",
    responsable = "",
    revision = "1",
    fechaCreacion = parseBackendDate(fecha),
    fechaEnvio = parseBackendDateOrNull(fecha),
    estado = estado.toDiagnosticStatus(),
    clienteNombre = usuario_nombre.orEmpty(),
    tipoInmueble = proyecto_tipo_inmueble.orEmpty(),
    requeridoPct = score?.toInt(),
    requeridoPctOficial = score?.toInt()
)

fun DiagnosticoPendienteDto.toLightEntity(): DiagnosticEntity = DiagnosticEntity(
    id = id,
    clienteId = 0L,
    projectName = proyecto.orEmpty(),
    ubicacion = "",
    responsable = "",
    revision = "1",
    fechaCreacion = parseBackendDate(fecha),
    fechaEnvio = parseBackendDateOrNull(fecha),
    estado = estado.toDiagnosticStatus(),
    clienteNombre = cliente.orEmpty(),
    tipoInmueble = tipoInmueble.orEmpty(),
    requeridoPct = porcentajeCumplimiento?.toInt(),
    requeridoPctOficial = porcentajeCumplimiento?.toInt()
)
