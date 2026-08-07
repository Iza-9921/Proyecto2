package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito
import kotlinx.coroutines.flow.Flow

/**
 * Catálogo de secciones/preguntas. Cada `tipo` de inmueble tiene su propio
 * catálogo independiente (igual que `scorecardStore.js` en la web, con
 * clave `ta_secciones::<tipo>`); todo método recibe el `tipo` sobre el que
 * opera.
 */
interface QuestionCatalogRepository {
    fun observeSections(tipo: String): Flow<List<SectionEntity>>
    suspend fun getAllSections(tipo: String): List<SectionEntity>
    fun observeQuestions(tipo: String): Flow<List<QuestionEntity>>
    suspend fun getAllQuestions(tipo: String): List<QuestionEntity>
    suspend fun getQuestionsForSection(tipo: String, sectionId: String): List<QuestionEntity>
    suspend fun updateQuestion(question: QuestionEntity)

    suspend fun addSeccion(tipo: String, icono: String, tituloLargo: String, tituloCorto: String): SectionEntity
    suspend fun updateSeccion(tipo: String, seccionId: String, icono: String, tituloLargo: String, tituloCorto: String)

    /** Borra la sección y todas sus preguntas (cascada, igual que en la web). */
    suspend fun deleteSeccion(tipo: String, seccionId: String)

    suspend fun addPregunta(
        tipo: String,
        seccionId: String,
        concepto: String,
        credito: Credito,
        admiteFoto: Boolean,
        descripcion: String = "",
        imagenEjemplo: String? = null
    ): QuestionEntity

    suspend fun deletePregunta(tipo: String, codigo: String)

    /** Reemplaza el catálogo completo de un tipo por su set de ejemplo (o lo deja vacío si no tiene). */
    suspend fun restaurarEjemplo(tipo: String)
}
