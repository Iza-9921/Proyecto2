package com.example.todoaccesible.core.util

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Validación básica de formato (no de existencia real): evita mandar al backend algo como "asdf". */
fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
