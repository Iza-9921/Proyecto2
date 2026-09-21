package com.example.todoaccesible.data.local.memory

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Tabla simple en memoria (se pierde al cerrar la app). De la época pre-backend-real; ahora que
 * usuarios/catálogo de preguntas/etc. vienen del backend por Retrofit, el único uso que le queda es
 * [com.example.todoaccesible.data.repository.impl.PresenceRepositoryImpl] (presencia "quién más está
 * viendo este diagnóstico", que no tiene ningún equivalente en el backend y se deja local a propósito).
 */
class InMemoryTable<T>(initial: List<T> = emptyList()) {
    private val state = MutableStateFlow(initial)

    private val idSeq = AtomicLong(initial.size.toLong())

    val flow: StateFlow<List<T>> get() = state

    val snapshot: List<T> get() = state.value

    fun nextId(): Long = idSeq.incrementAndGet()

    fun mutate(block: (List<T>) -> List<T>) {
        state.update(block)
    }
}
