package com.example.todoaccesible.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * RF-18: control de sesiones activas para evitar el uso compartido de una
 * misma licencia. Sin backend ni base de datos, esto se simula en memoria:
 * un usuario no puede tener dos sesiones activas al mismo tiempo salvo que
 * se fuerce el cierre de la anterior. Se reinicia al cerrar la app.
 */
class ActiveSessionRegistry {
    private val active = MutableStateFlow<Set<Long>>(emptySet())

    fun isActive(userId: Long): Boolean = active.value.contains(userId)

    fun markActive(userId: Long) {
        active.update { it + userId }
    }

    fun markInactive(userId: Long) {
        active.update { it - userId }
    }

    fun observeActiveUserIds(): Flow<Set<Long>> = active
}
