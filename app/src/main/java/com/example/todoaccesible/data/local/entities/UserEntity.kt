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
    val diagnosticosDisponibles: Int? = 0
)
