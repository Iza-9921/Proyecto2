package com.example.todoaccesible.data.local.entities

/**
 * Sección de un cuestionario. Cada [tipo] de inmueble tiene su propio
 * catálogo independiente de secciones (p.ej. "Otro" usa las 8 secciones
 * fijas del Scorecard v2.6; el resto tiene sus propios sets más chicos),
 * así que [id] solo es único dentro de un mismo [tipo]. [nombre] es el
 * título largo de la sección; [icono] y [tituloCorto] son opcionales
 * (mirroring `seccion.icono`/`tituloCorto` en la web), usados en el
 * catálogo editable y en la navegación por categorías del cuestionario.
 */
data class SectionEntity(
    val id: String,
    val tipo: String,
    val nombre: String,
    val orden: Int,
    val icono: String = "",
    val tituloCorto: String = ""
)
