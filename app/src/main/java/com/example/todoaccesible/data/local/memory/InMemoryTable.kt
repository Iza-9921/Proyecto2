package com.example.todoaccesible.data.local.memory

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Reemplaza a Room/SQLite: guarda una lista en memoria (se pierde al cerrar la
 * app, no hay backend ni base de datos). `initial` sirve para sembrar datos
 * por defecto (usuario admin, catálogo de preguntas, etc.) al construir el
 * AppContainer.
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
