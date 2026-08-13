package com.example.todoaccesible.data.remote.dto

data class IniciarDiagnosticoRequest(val proyecto_id: Long)

data class IniciarDiagnosticoResponseDto(
    val id: Long,
    val proyecto_id: Long,
    val usuario_id: Long,
    val estado: String,
    val mensaje: String? = null
)

/** Fila cruda de la tabla `diagnosticos`, tal como la devuelve el backend (más `nombre_proyecto` cuando viene de un JOIN). */
data class DiagnosticoRawDto(
    val id: Long,
    val proyecto_id: Long,
    val usuario_id: Long,
    val fecha_inicio: String? = null,
    val fecha_fin: String? = null,
    val resultado_preliminar: Double? = null,
    val resultado_final: Double? = null,
    val recomendaciones_finales: String? = null,
    val estado: String,
    val motivo_rechazo: String? = null,
    /** Comentario del admin por pregunta al solicitar información adicional (`{preguntaId: mensaje}`). Viene en la fila cruda de `diagnosticos`, disponible también para el rol cliente vía `obtenerParaCliente`. */
    val observaciones_especialista: Map<String, String>? = null,
    /** Validación del admin por pregunta (`{preguntaId: "aprobado"|"pendiente"|"no_cumple"|"no_aplica"}`). Igual que `observaciones_especialista`, viene en la fila cruda y por lo tanto también llega en el shape cliente. */
    val evaluaciones: Map<String, String>? = null,
    val calificacion_notificada: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null,
    val nombre_proyecto: String? = null
)

data class ListarDiagnosticosResponseDto(
    val rows: List<DiagnosticoRawDto> = emptyList(),
    val total: Int = 0
)

data class PreguntaConRespuestaDto(
    val categoria_id: Long,
    val categoria_nombre: String,
    val pregunta_id: Long,
    val pregunta_texto: String,
    val ponderacion: Double = 0.0,
    val orden: Int = 0,
    val respuesta_id: Long? = null,
    val respuesta: String? = null,
    val observaciones: String? = null
)

data class RespuestaDto(
    val id: Long,
    val diagnostico_id: Long,
    val pregunta_id: Long,
    val respuesta: String,
    val observaciones: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class EvidenciaDto(
    val id: Long,
    val respuesta_id: Long,
    val ruta_imagen: String? = null,
    val thumbnail_url: String? = null,
    val is_sensitive: Boolean = false,
    val fecha_subida: String? = null
)

/** Shape para `GET /diagnosticos/:id` cuando lo pide un rol `cliente`. */
data class DiagnosticoClienteDetailDto(
    val diagnostico: DiagnosticoRawDto,
    val preguntas: List<PreguntaConRespuestaDto> = emptyList(),
    val respuestas: List<RespuestaDto> = emptyList(),
    val evidencias: List<EvidenciaDto> = emptyList()
)

data class RespuestaEvaluadaAdminDto(
    val valor: String? = null,
    val comentario: String? = null,
    val fotos: List<String> = emptyList()
)

data class HistorialEntryDto(
    val version: Int = 0,
    val fecha: String? = null,
    val cambios: String = ""
)

/** Shape para `GET /diagnosticos/:id` cuando lo pide un rol `admin` (`obtenerParaAdmin`). */
data class DiagnosticoAdminDetailDto(
    val id: Long,
    val nombre: String? = null,
    val direccion: String? = null,
    val tipo: String? = null,
    val clienteId: String? = null,
    val cliente: String? = null,
    val fecha: String? = null,
    val fechaEnvio: String? = null,
    val status: String,
    val respuestas: Map<String, RespuestaEvaluadaAdminDto> = emptyMap(),
    val observacionesEspecialista: Map<String, String>? = null,
    val motivoRechazo: String? = null,
    val historial: List<HistorialEntryDto> = emptyList(),
    val evaluaciones: Map<String, String>? = null,
    val calificacionNotificada: Boolean = false
)

data class RespuestaInputDto(
    val pregunta_id: Long,
    val respuesta: String,
    val observaciones: String? = null,
    val fotos: List<String>? = null,
    val is_sensitive: Boolean? = null
)

data class GuardarRespuestasRequest(val respuestas: List<RespuestaInputDto>)

data class RespuestaGuardadaDto(
    val pregunta_id: Long,
    val respuesta_id: Long,
    val fotos: List<String> = emptyList()
)

data class GuardarRespuestasResponseDto(
    val guardados: Int = 0,
    val respuestas: List<RespuestaGuardadaDto> = emptyList()
)

data class PuntuacionCategoriaDto(
    val categoria_id: Long,
    val categoria_nombre: String,
    val total_ponderacion: Double = 0.0,
    val obtenido_ponderacion: Double = 0.0,
    val porcentaje: Double = 0.0,
    val total_preguntas: Int = 0,
    val respondidas: Int = 0,
    val no_aplica: Int = 0
)

data class FinalizarResponseDto(
    val resultado_preliminar: Double = 0.0,
    val resultado_por_categoria: List<PuntuacionCategoriaDto> = emptyList(),
    val estado: String,
    val mensaje: String? = null
)

data class EstadoDiagnosticoDto(
    val id: Long,
    val estado: String,
    val resultado_preliminar: Double? = null,
    val resultado_final: Double? = null,
    val recomendaciones_finales: String? = null,
    val fecha_inicio: String? = null,
    val fecha_fin: String? = null,
    val solicitud_info_mensaje: String? = null
)

data class EvaluacionesRequest(val evaluaciones: Map<String, String>)

data class AprobarRequest(val observaciones: Map<String, String> = emptyMap())

data class RechazarRequest(val motivo: String, val observaciones: Map<String, String> = emptyMap())

data class SolicitarInfoRequest(val mensaje: String, val observaciones: Map<String, String> = emptyMap())

data class ActualizarEstadoRequest(val estado: String)

data class MensajeEstadoResponseDto(val mensaje: String? = null, val estado: String? = null)
