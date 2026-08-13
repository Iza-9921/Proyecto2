package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.DiagnosticHistoryEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.DiagnosticoApiService
import com.example.todoaccesible.data.remote.mapper.parseBackendDate
import com.example.todoaccesible.data.remote.mapper.toDiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Deriva el historial de `historial: [{version, fecha, cambios}]` del
 * detalle ADMIN (mismo hueco de contrato que [QuestionReviewRepositoryImpl]:
 * el shape CLIENTE no trae historial en absoluto, así que para una sesión
 * cliente esto siempre queda vacío).
 *
 * El backend manda una sola frase humana (`cambios`) en vez de campos
 * estructurados: en vez de intentar parsearla, se deja `previousStatus` en
 * `null` y `newStatus` como el estado ACTUAL del diagnóstico (aproximación:
 * no es exacto por fila, pero mantiene la pantalla mostrando algo legible en
 * vez de un chip vacío); la frase completa va íntegra en `comentario`, que es
 * lo único que la pantalla realmente necesita mostrar con fidelidad.
 * `reviewerId` queda `null` siempre (el backend no expone quién hizo cada
 * cambio como id) — las pantallas ya resuelven ese caso mostrando "Cliente"
 * como texto por defecto.
 *
 * `record()` es no-op: el backend ya escribe su propio historial como
 * efecto secundario de aprobar/rechazar/solicitar-info/etc, así que
 * [DiagnosticRepositoryImpl] nunca lo llama.
 */
class DiagnosticHistoryRepositoryImpl(
    private val diagnosticoApi: DiagnosticoApiService,
    private val sessionManager: SessionManager
) : DiagnosticHistoryRepository {

    private val _byDiagnostic = MutableStateFlow<Map<Long, List<DiagnosticHistoryEntity>>>(emptyMap())
    private val loaded = ConcurrentHashMap.newKeySet<Long>()

    override suspend fun record(
        diagnosticId: Long,
        previousStatus: DiagnosticStatus?,
        newStatus: DiagnosticStatus,
        reviewerId: Long?,
        comentario: String
    ) = Unit

    override fun observeForDiagnostic(diagnosticId: Long): Flow<List<DiagnosticHistoryEntity>> =
        _byDiagnostic
            .onStart { if (loaded.add(diagnosticId)) runCatching { refresh(diagnosticId) } }
            .map { it[diagnosticId].orEmpty() }

    private suspend fun refresh(diagnosticId: Long) {
        if (sessionManager.session.first()?.rol != Role.ADMIN) return
        val dto = diagnosticoApi.obtenerParaAdmin(diagnosticId)
        val currentStatus = dto.status.toDiagnosticStatus()
        val entries = dto.historial.mapIndexed { index, h ->
            DiagnosticHistoryEntity(
                id = diagnosticId * 10_000 + index,
                diagnosticId = diagnosticId,
                previousStatus = null,
                newStatus = currentStatus,
                reviewerId = null,
                comentario = h.cambios,
                fecha = parseBackendDate(h.fecha)
            )
        }
        _byDiagnostic.update { it + (diagnosticId to entries) }
    }
}
