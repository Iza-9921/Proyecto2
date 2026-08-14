package com.example.todoaccesible.data.remote

import com.example.todoaccesible.BuildConfig
import com.example.todoaccesible.data.local.entities.NotificationEntity
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

/**
 * Envuelve un [Socket] de Socket.IO conectado a `BuildConfig.SOCKET_BASE_URL`
 * (mismo host:puerto que la API, sin `/api`, path `/socket.io/` por
 * defecto). Alcance foreground-only: [connect]/[disconnect] los maneja
 * `AppContainer` según haya o no sesión, no hay WorkManager ni servicio en
 * background.
 *
 * El servidor une automáticamente al usuario a la room `user_<id>` (y a
 * `especialistas` si es admin) en base al `token` mandado en `auth`; no hay
 * eventos cliente->servidor que emitir aparte de la conexión/desconexión.
 */
class SocketManager(
    private val onNotificacion: (NotificationEntity) -> Unit,
    private val onDiagnosticoEvent: (Long) -> Unit
) {
    companion object {
        /** `estado_actualizado`/`nuevo_diagnostico_pendiente` son solo para admins (room `especialistas`); el resto van a `user_<id>`. */
        private val DIAGNOSTIC_EVENTS = listOf(
            "estado_actualizado",
            "nuevo_diagnostico_pendiente",
            "info_requerida",
            "diagnostico_validado",
            "resultado_final_listo",
            "respuesta_recibida",
            "revision_asignada"
        )
    }

    private var socket: Socket? = null
    private var connectedUserId: Long? = null

    fun connect(userId: Long, accessToken: String) {
        if (connectedUserId == userId && socket?.connected() == true) return
        disconnect()

        val options = IO.Options().apply {
            auth = mapOf("token" to accessToken)
            reconnection = true
        }
        val newSocket = IO.socket(BuildConfig.SOCKET_BASE_URL, options)

        newSocket.on("notificacion_nueva") { args -> handleNotificacion(args, userId) }
        DIAGNOSTIC_EVENTS.forEach { evento -> newSocket.on(evento) { args -> handleDiagnosticoEvent(args) } }

        newSocket.connect()
        socket = newSocket
        connectedUserId = userId
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        connectedUserId = null
    }

    private fun handleNotificacion(args: Array<Any>, fallbackUserId: Long) {
        val json = args.getOrNull(0) as? JSONObject ?: return
        val entity = NotificationEntity(
            id = json.optLong("id"),
            diagnosticId = json.optLong("diagnosticoId", -1L).takeIf { it > 0 },
            destinatarioId = if (json.has("destinatarioId")) json.optLong("destinatarioId", fallbackUserId) else fallbackUserId,
            tipo = json.optString("evento", json.optString("tipo", json.optString("categoria", "notificacion"))),
            mensaje = json.optString("cuerpo", json.optString("titulo", "")),
            leido = json.optBoolean("leida", false),
            fecha = System.currentTimeMillis()
        )
        onNotificacion(entity)
    }

    /**
     * Payload corto (`{diagnostico_id, mensaje, timestamp, ...}`): en vez de
     * parchear campo por campo, dispara un refetch completo de
     * `GET /diagnosticos/:id` para ese id (más simple y robusto).
     */
    private fun handleDiagnosticoEvent(args: Array<Any>) {
        val json = args.getOrNull(0) as? JSONObject ?: return
        val diagnosticoId = json.optLong("diagnostico_id", json.optLong("diagnosticoId", -1L))
        if (diagnosticoId > 0) onDiagnosticoEvent(diagnosticoId)
    }
}
