package com.example.todoaccesible.data.remote.mapper

import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.QuestionReviewStatus
import com.example.todoaccesible.data.model.Role
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Traducciones String (backend) <-> enum (Kotlin) hechas a mano, sin adapter
 * automático de Gson: los nombres de los enums en Kotlin no coinciden 1:1
 * con los valores reales del backend (confirmados leyendo el código fuente,
 * no `/api-docs`).
 */

// ---- DiagnosticStatus <-> DiagnosticoEstado ('borrador'|'enviado'|'en_revision'|'informacion_adicional_requerida'|'validado'|'rechazado') ----

fun DiagnosticStatus.toBackend(): String = when (this) {
    DiagnosticStatus.BORRADOR -> "borrador"
    DiagnosticStatus.PENDIENTE -> "enviado"
    DiagnosticStatus.EN_REVISION -> "en_revision"
    DiagnosticStatus.INFO_REQUERIDA -> "informacion_adicional_requerida"
    DiagnosticStatus.RECHAZADO -> "rechazado"
    DiagnosticStatus.VALIDADO -> "validado"
}

fun String.toDiagnosticStatus(): DiagnosticStatus = when (this) {
    "borrador" -> DiagnosticStatus.BORRADOR
    "enviado" -> DiagnosticStatus.PENDIENTE
    "en_revision" -> DiagnosticStatus.EN_REVISION
    "informacion_adicional_requerida" -> DiagnosticStatus.INFO_REQUERIDA
    "rechazado" -> DiagnosticStatus.RECHAZADO
    "validado" -> DiagnosticStatus.VALIDADO
    else -> DiagnosticStatus.BORRADOR
}

// ---- AnswerValue <-> RespuestaValor ('aprobado'|'pendiente'|'no_cumple'|'no_aplica') ----

fun AnswerValue.toBackend(): String = when (this) {
    AnswerValue.APROBADO -> "aprobado"
    AnswerValue.PENDIENTE -> "pendiente"
    AnswerValue.NO_CUMPLE -> "no_cumple"
    AnswerValue.NO_APLICA -> "no_aplica"
}

fun String.toAnswerValueBackend(): AnswerValue? = when (this) {
    "aprobado" -> AnswerValue.APROBADO
    "pendiente" -> AnswerValue.PENDIENTE
    "no_cumple" -> AnswerValue.NO_CUMPLE
    "no_aplica" -> AnswerValue.NO_APLICA
    else -> null
}

// ---- QuestionReviewStatus -> AnswerValue (backend RespuestaValor), para `POST /:id/evaluaciones' ----
// Inverso de `QuestionReviewStatus.toAnswerValue()` en domain/scoring/QuestionReviewMapper.kt (no se toca ese
// archivo). SOLICITAR_INFO no tiene equivalente en el backend (roundtrip con pérdida: vuelve como PENDIENTE).

fun AnswerValue.toReviewStatus(): QuestionReviewStatus = when (this) {
    AnswerValue.APROBADO -> QuestionReviewStatus.APROBADO
    AnswerValue.NO_APLICA -> QuestionReviewStatus.NO_APLICA
    AnswerValue.NO_CUMPLE -> QuestionReviewStatus.NO_CUMPLE
    AnswerValue.PENDIENTE -> QuestionReviewStatus.PENDIENTE
}

// ---- Credito <-> 'Required'|'Plus' ----

fun Credito.toBackend(): String = when (this) {
    Credito.REQUIRED -> "Required"
    Credito.PLUS -> "Plus"
}

fun String.toCredito(): Credito = if (equals("Plus", ignoreCase = true)) Credito.PLUS else Credito.REQUIRED

// ---- Role <-> 'admin'|'cliente' ----

fun Role.toBackend(): String = if (this == Role.ADMIN) "admin" else "cliente"

fun String.toRole(): Role = if (equals("admin", ignoreCase = true)) Role.ADMIN else Role.CLIENTE

// ---- Fechas: el backend manda distintos formatos (ISO 8601, "YYYY-MM-DD HH:mm", "YYYY-MM-DD") ----

private val dateFormats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm",
    "yyyy-MM-dd"
)

/** Devuelve `null` si no se pudo parsear (para poder usar `?:` con un valor por defecto). */
fun parseBackendDateOrNull(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    for (pattern in dateFormats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            if (pattern.endsWith("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.parse(value)?.time
        } catch (_: Exception) {
            // probar el siguiente patrón
        }
    }
    return null
}

fun parseBackendDate(value: String?, fallback: Long = System.currentTimeMillis()): Long =
    parseBackendDateOrNull(value) ?: fallback
