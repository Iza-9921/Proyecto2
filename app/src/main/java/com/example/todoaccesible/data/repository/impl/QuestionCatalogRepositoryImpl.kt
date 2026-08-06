package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.local.seed.CuestionarioEjemploSeeder
import com.example.todoaccesible.data.local.seed.QuestionCatalogSeeder
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import kotlinx.coroutines.flow.map

/**
 * Catálogo multi-tipo: cada "tipo de inmueble" tiene su propio set
 * independiente de secciones/preguntas (equivalente a `scorecardStore.js`
 * en la web). Se guarda todo en dos tablas planas filtradas por `tipo` en
 * cada lectura -- más simple que separar una tabla por tipo, y evita
 * reestructurar `InMemoryTable`. Sembrado al construirse para los tipos
 * fijos por defecto ([TipoCuestionarioRepositoryImpl.defaultTipos]): "Otro"
 * con el catálogo fijo de 187 preguntas, el resto con su set de ejemplo. Un
 * tipo creado luego por el admin arranca vacío (se agrega vía [addSeccion]).
 */
class QuestionCatalogRepositoryImpl(
    private val sections: InMemoryTable<SectionEntity> = InMemoryTable(seedAllSections()),
    private val questions: InMemoryTable<QuestionEntity> = InMemoryTable(seedAllQuestions())
) : QuestionCatalogRepository {

    companion object {
        private fun seedAllSections(): List<SectionEntity> =
            TipoCuestionarioRepositoryImpl.defaultTipos().flatMap { sectionsFor(it.nombre) }

        private fun seedAllQuestions(): List<QuestionEntity> =
            TipoCuestionarioRepositoryImpl.defaultTipos().flatMap { questionsFor(it.nombre) }

        private fun sectionsFor(tipo: String): List<SectionEntity> = when {
            tipo == QuestionCatalogSeeder.TIPO -> QuestionCatalogSeeder.sectionEntities(tipo)
            CuestionarioEjemploSeeder.tieneEjemplo(tipo) -> CuestionarioEjemploSeeder.sectionEntities(tipo)
            else -> emptyList()
        }

        private fun questionsFor(tipo: String): List<QuestionEntity> = when {
            tipo == QuestionCatalogSeeder.TIPO -> QuestionCatalogSeeder.questionEntities(tipo)
            CuestionarioEjemploSeeder.tieneEjemplo(tipo) -> CuestionarioEjemploSeeder.questionEntities(tipo)
            else -> emptyList()
        }
    }

    override fun observeSections(tipo: String) =
        sections.flow.map { list -> list.filter { it.tipo == tipo }.sortedBy { it.orden } }

    override suspend fun getAllSections(tipo: String) =
        sections.snapshot.filter { it.tipo == tipo }.sortedBy { it.orden }

    override fun observeQuestions(tipo: String) =
        questions.flow.map { list -> list.filter { it.tipo == tipo }.sortedBy { it.orden } }

    override suspend fun getAllQuestions(tipo: String) =
        questions.snapshot.filter { it.tipo == tipo }.sortedBy { it.orden }

    override suspend fun getQuestionsForSection(tipo: String, sectionId: String) =
        questions.snapshot.filter { it.tipo == tipo && it.seccionId == sectionId }.sortedBy { it.orden }

    override suspend fun updateQuestion(question: QuestionEntity) {
        questions.mutate { list -> list.map { if (it.tipo == question.tipo && it.codigo == question.codigo) question else it } }
    }

    override suspend fun addSeccion(tipo: String, icono: String, tituloLargo: String, tituloCorto: String): SectionEntity {
        val existentes = sections.snapshot.filter { it.tipo == tipo }
        val siguienteOrden = (existentes.maxOfOrNull { it.orden } ?: -1) + 1
        val id = "seccion_${sections.nextId()}"
        val nueva = SectionEntity(id = id, tipo = tipo, nombre = tituloLargo, orden = siguienteOrden, icono = icono, tituloCorto = tituloCorto)
        sections.mutate { it + nueva }
        return nueva
    }

    override suspend fun updateSeccion(tipo: String, seccionId: String, icono: String, tituloLargo: String, tituloCorto: String) {
        sections.mutate { list ->
            list.map {
                if (it.tipo == tipo && it.id == seccionId) it.copy(nombre = tituloLargo, icono = icono, tituloCorto = tituloCorto) else it
            }
        }
    }

    override suspend fun deleteSeccion(tipo: String, seccionId: String) {
        sections.mutate { list -> list.filterNot { it.tipo == tipo && it.id == seccionId } }
        questions.mutate { list -> list.filterNot { it.tipo == tipo && it.seccionId == seccionId } }
    }

    override suspend fun addPregunta(
        tipo: String,
        seccionId: String,
        concepto: String,
        credito: Credito,
        admiteFoto: Boolean,
        descripcion: String,
        imagenEjemplo: String?
    ): QuestionEntity {
        val delaSeccion = questions.snapshot.filter { it.tipo == tipo && it.seccionId == seccionId }
        val siguienteNumero = delaSeccion.size + 1
        val codigo = "$seccionId.${siguienteNumero.toString().padStart(2, '0')}"
        val siguienteOrden = (questions.snapshot.filter { it.tipo == tipo }.maxOfOrNull { it.orden } ?: -1) + 1
        val nueva = QuestionEntity(
            codigo = codigo,
            tipo = tipo,
            seccionId = seccionId,
            concepto = concepto,
            credito = credito,
            admiteFoto = admiteFoto,
            orden = siguienteOrden,
            descripcion = descripcion,
            imagenEjemplo = imagenEjemplo
        )
        questions.mutate { it + nueva }
        return nueva
    }

    override suspend fun deletePregunta(tipo: String, codigo: String) {
        questions.mutate { list -> list.filterNot { it.tipo == tipo && it.codigo == codigo } }
    }

    override suspend fun restaurarEjemplo(tipo: String) {
        sections.mutate { list -> list.filterNot { it.tipo == tipo } + sectionsFor(tipo) }
        questions.mutate { list -> list.filterNot { it.tipo == tipo } + questionsFor(tipo) }
    }
}
