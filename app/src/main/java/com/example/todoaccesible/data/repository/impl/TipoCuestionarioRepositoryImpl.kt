package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.TipoCuestionarioEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import kotlinx.coroutines.flow.map

class TipoCuestionarioRepositoryImpl(
    private val tipos: InMemoryTable<TipoCuestionarioEntity> = InMemoryTable(defaultTipos())
) : TipoCuestionarioRepository {

    companion object {
        /** Mismo set de tipos fijos que la web (`TIPOS_INMUEBLE` en categoriasMock.js). */
        fun defaultTipos(): List<TipoCuestionarioEntity> = listOf(
            "Edificio de oficinas",
            "Comercio",
            "Vivienda unifamiliar",
            "Edificio residencial",
            "Espacio público",
            "Local comercial",
            "Otro"
        ).mapIndexed { index, nombre -> TipoCuestionarioEntity(nombre = nombre, orden = index) }
    }

    override fun observeTipos() = tipos.flow.map { list -> list.sortedBy { it.orden }.map { it.nombre } }

    override suspend fun getTipos(): List<String> = tipos.snapshot.sortedBy { it.orden }.map { it.nombre }

    override suspend fun addTipo(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty() || tipos.snapshot.any { it.nombre == limpio }) return
        val siguienteOrden = (tipos.snapshot.maxOfOrNull { it.orden } ?: -1) + 1
        tipos.mutate { it + TipoCuestionarioEntity(nombre = limpio, orden = siguienteOrden) }
    }

    override suspend fun deleteTipo(nombre: String) {
        if (tipos.snapshot.size <= 1) return
        tipos.mutate { list -> list.filterNot { it.nombre == nombre } }
    }
}
