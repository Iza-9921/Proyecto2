package com.example.todoaccesible.data.remote.dto

/** `GET /admin/usuarios` — ojo: `id` viene como STRING aquí, a diferencia del `id` numérico de auth. */
data class AdminUsuarioDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val activo: Boolean,
    val empresa: EmpresaInfoDto? = null,
    val cuestionarioAsignado: String? = null,
    val limiteCuestionarios: Int = 0,
    val createdAt: String? = null,
    val diagnosticosCount: Int = 0
)

data class ActivoRequest(val activo: Boolean)

data class CuestionarioAsignadoRequest(val tipo: String)

data class LimiteCuestionariosResponseDto(val limiteCuestionarios: Int = 0)

data class DiagnosticoPendienteDto(
    val id: Long,
    val proyecto: String? = null,
    val cliente: String? = null,
    val fecha: String? = null,
    val estado: String,
    val tipoInmueble: String? = null,
    val porcentajeCumplimiento: Double? = null
)

data class AdminDiagnosticoRowDto(
    val diagnostico_id: Long,
    val score: Double? = null,
    val estado: String,
    val fecha: String? = null,
    val usuario_id: Long? = null,
    val usuario_nombre: String? = null,
    val usuario_email: String? = null,
    val usuario_empresa: String? = null,
    val proyecto_id: Long? = null,
    val proyecto_nombre: String? = null,
    val proyecto_tipo_inmueble: String? = null
)

data class AdminDiagnosticosResponseDto(
    val rows: List<AdminDiagnosticoRowDto> = emptyList(),
    val total: Int = 0
)
