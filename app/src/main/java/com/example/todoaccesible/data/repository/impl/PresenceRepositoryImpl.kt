package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.PresenceEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.repository.PresenceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class PresenceRepositoryImpl(
    private val presence: InMemoryTable<PresenceEntity> = InMemoryTable()
) : PresenceRepository {

    companion object {
        private const val STALE_AFTER_MS = 60_000L
        private const val TICK_MS = 5_000L
    }

    /** Re-evalúa la lista de vigentes cada [TICK_MS], incluso sin un nuevo latido, para que un visor expire pasados [STALE_AFTER_MS]. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MS)
        }
    }

    override fun observeViewers(diagnosticId: Long, excludeUserId: Long): Flow<List<String>> =
        combine(presence.flow, ticker) { list, now ->
            list.filter { it.diagnosticId == diagnosticId && it.userId != excludeUserId && now - it.timestampMs < STALE_AFTER_MS }
                .map { it.userName }
                .distinct()
        }

    override suspend fun heartbeat(diagnosticId: Long, userId: Long, userName: String) {
        presence.mutate { list ->
            list.filterNot { it.diagnosticId == diagnosticId && it.userId == userId } +
                PresenceEntity(diagnosticId, userId, userName, System.currentTimeMillis())
        }
    }

    override suspend fun clear(diagnosticId: Long, userId: Long) {
        presence.mutate { list -> list.filterNot { it.diagnosticId == diagnosticId && it.userId == userId } }
    }
}
