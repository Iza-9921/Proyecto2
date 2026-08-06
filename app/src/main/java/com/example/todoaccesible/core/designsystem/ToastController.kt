package com.example.todoaccesible.core.designsystem

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class ToastTipo { INFO, ALERTA, EXITO, ERROR }

data class ToastMessage(val texto: String, val tipo: ToastTipo = ToastTipo.INFO)

/**
 * Emisor de avisos pasivos (equivalente a `ToastNotification.jsx` +
 * `showToast` de `SocketContext.jsx` en la web), consumido una sola vez
 * desde `MainActivity` con un `SnackbarHost` en la raíz de la app. Antes de
 * este controlador, toda la retroalimentación en la app usaba
 * `AlertDialog`s bloqueantes; úsalo para confirmaciones de una sola acción
 * (p.ej. "Pregunta eliminada") en vez de otro diálogo.
 */
class ToastController {
    private val _messages = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<ToastMessage> = _messages

    fun show(texto: String, tipo: ToastTipo = ToastTipo.INFO) {
        _messages.tryEmit(ToastMessage(texto, tipo))
    }
}
