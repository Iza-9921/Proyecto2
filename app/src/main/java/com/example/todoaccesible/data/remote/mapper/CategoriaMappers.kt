package com.example.todoaccesible.data.remote.mapper

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.remote.dto.SeccionDto

fun SeccionDto.toEntity(tipo: String): SectionEntity = SectionEntity(
    id = id.toString(),
    tipo = tipo,
    nombre = tituloLargo,
    orden = numero,
    icono = icono,
    tituloCorto = tituloCorto
)

fun SeccionDto.questionEntities(tipo: String): List<QuestionEntity> =
    preguntas.mapIndexed { index, p ->
        QuestionEntity(
            codigo = p.id.toString(),
            tipo = tipo,
            seccionId = id.toString(),
            concepto = p.concepto,
            credito = p.credito.toCredito(),
            admiteFoto = p.foto,
            orden = index,
            descripcion = p.descripcion.orEmpty(),
            imagenEjemplo = p.imagenEjemplo
        )
    }
