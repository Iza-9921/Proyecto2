package com.example.todoaccesible.data.local.seed

import com.example.todoaccesible.data.model.Credito
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionCatalogSeederTest {

    // Totales Required/Plus por sección tal como aparecen en la portada de
    // referencia del Scorecard v2.6 (Cuantificación parcial del proyecto).
    private val expectedTotals = mapOf(
        "1" to (11 to 11),
        "2" to (8 to 8),
        "3" to (23 to 15),
        "4" to (14 to 22),
        "5" to (12 to 6),
        "6" to (16 to 16),
        "7" to (2 to 9),
        "8" to (10 to 4)
    )

    @Test
    fun `8 secciones fijas en el orden correcto`() {
        val sections = QuestionCatalogSeeder.sectionEntities()
        assertEquals(8, sections.size)
        assertEquals((1..8).map { it.toString() }, sections.sortedBy { it.orden }.map { it.id })
    }

    @Test
    fun `total de preguntas es 187`() {
        assertEquals(187, QuestionCatalogSeeder.questionEntities().size)
    }

    @Test
    fun `codigos son unicos`() {
        val codigos = QuestionCatalogSeeder.questionEntities().map { it.codigo }
        assertEquals(codigos.size, codigos.toSet().size)
    }

    @Test
    fun `totales required y plus por seccion coinciden con la portada de referencia`() {
        val questions = QuestionCatalogSeeder.questionEntities()
        expectedTotals.forEach { (sectionId, expected) ->
            val (expectedRequired, expectedPlus) = expected
            val sectionQuestions = questions.filter { it.seccionId == sectionId }
            val required = sectionQuestions.count { it.credito == Credito.REQUIRED }
            val plus = sectionQuestions.count { it.credito == Credito.PLUS }
            assertEquals("Required en sección $sectionId", expectedRequired, required)
            assertEquals("Plus en sección $sectionId", expectedPlus, plus)
        }
    }

    @Test
    fun `ningun concepto esta vacio`() {
        assertTrue(QuestionCatalogSeeder.questionEntities().all { it.concepto.isNotBlank() })
    }
}
