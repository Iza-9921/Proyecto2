package com.example.todoaccesible.core.util

import java.security.MessageDigest

/**
 * Hash local simple (SHA-256) para credenciales guardadas en Room mientras
 * no exista un backend que maneje el auth real. No usar como referencia de
 * seguridad para producción con datos sensibles de verdad.
 */
object PasswordHasher {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun matches(password: String, expectedHash: String): Boolean = hash(password) == expectedHash
}
