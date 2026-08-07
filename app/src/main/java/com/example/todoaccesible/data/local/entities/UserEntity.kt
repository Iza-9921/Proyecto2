package com.example.todoaccesible.data.local.entities

import com.example.todoaccesible.data.model.Role

data class UserEntity(
    val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val nombre: String,
    val rol: Role,
    val licenseActive: Boolean = true,
    /**
     * Cuántos diagnósticos nuevos puede iniciar este cliente; lo asigna el
     * administrador manualmente (el pago se hace fuera de la app, de forma
     * presencial). `null` = ilimitados. Por defecto 0: un cliente recién
     * creado no puede iniciar ningún diagnóstico hasta que el administrador
     * le asigne cupo.
     */
    val diagnosticosDisponibles: Int? = 0,
    /** Fecha de alta de la cuenta; usado para agrupar la lista de usuarios por mes. */
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Cuestionario (tipo de inmueble) que este cliente debe responder,
     * asignado manualmente por el admin al activar la cuenta. `null` = aún
     * no asignado explícitamente (se usa el primer tipo disponible).
     */
    val cuestionarioAsignado: String? = null
)
