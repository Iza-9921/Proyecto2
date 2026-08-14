package com.example.todoaccesible.data.remote.dto

data class PreguntaDto(
    val id: Long,
    /** Crudo: "Required" | "Plus". */
    val credito: String,
    val foto: Boolean,
    val concepto: String,
    val descripcion: String? = null,
    val imagenEjemplo: String? = null
)

data class SeccionDto(
    val id: Long,
    val numero: Int = 0,
    val rango: String? = null,
    val icono: String = "",
    val tituloLargo: String,
    val tituloCorto: String = "",
    val preguntas: List<PreguntaDto> = emptyList()
)

data class SeccionRequest(
    val icono: String? = null,
    val tituloLargo: String? = null,
    val tituloCorto: String? = null,
    val rango: String? = null
)

data class OrdenRequest(val ids: List<Long>)

data class PreguntaRequest(
    val concepto: String? = null,
    val credito: String? = null,
    val foto: Boolean? = null,
    val descripcion: String? = null,
    val imagenEjemplo: String? = null
)

data class OkDto(val ok: Boolean = true)

data class TipoInmuebleRequest(val nombre: String)
