package com.example.todoaccesible.data.repository

import kotlinx.coroutines.flow.Flow

/** Lista de "tipos de inmueble" (cada uno con su propio cuestionario independiente). */
interface TipoCuestionarioRepository {
    fun observeTipos(): Flow<List<String>>
    suspend fun getTipos(): List<String>

    /** No hace nada si ya existe un tipo con ese nombre (comparación exacta, como en la web). */
    suspend fun addTipo(nombre: String)

    /** No permite borrar el último tipo restante. */
    suspend fun deleteTipo(nombre: String)

    /** Cambia el nombre de un tipo existente sin tocar sus secciones/preguntas (ligadas por id, no por nombre). */
    suspend fun renombrarTipo(nombreActual: String, nombreNuevo: String)
}
