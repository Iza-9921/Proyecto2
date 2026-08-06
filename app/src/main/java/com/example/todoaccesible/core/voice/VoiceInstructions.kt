package com.example.todoaccesible.core.voice

import com.example.todoaccesible.navigation.Routes

/**
 * Texto que lee la guía de voz por defecto en cada pantalla, cuando la
 * propia pantalla no lo sobreescribe con un texto más específico de su
 * estado (vía `VoiceGuideController.setInstructions`). Transcrito de
 * `src/data/voiceInstructions.js` (`INSTRUCCIONES_POR_RUTA`) en la web.
 */
object VoiceInstructions {

    private val porRuta: Map<String, String> = mapOf(
        Routes.Login.route to
            "Estás en la pantalla de inicio de sesión. Escribe tu correo electrónico y tu contraseña en los campos, y presiona el botón Ingresar. Si todavía no tienes cuenta, usa el enlace Regístrate que está debajo del formulario.",
        Routes.Register.route to
            "Estás en la pantalla de registro. El proceso tiene dos pasos: primero creas tu cuenta con tu nombre, correo y contraseña, y después registras los datos de tu empresa o inmueble. Presiona Siguiente para avanzar.",
        Routes.ClienteDashboard.route to
            "Estás en tu panel principal. Aquí ves la lista de tus diagnósticos de accesibilidad con su estado. Si todavía no has hecho ninguno, busca el botón para iniciar un nuevo diagnóstico.",
        Routes.ProjectInfo.route to
            "Estás creando un nuevo diagnóstico de accesibilidad. Primero llenas los datos generales del inmueble, después contestas las preguntas de cada sección, y al final puedes ver el resumen y exportarlo en PDF.",
        Routes.Questionnaire.route to
            "Estás contestando las preguntas del diagnóstico. Responde cada pregunta, agrega un comentario o una foto si aplica, y usa Siguiente o Anterior para moverte entre preguntas.",
        Routes.DiagnosticResult.route to
            "Estás viendo el resumen preliminar de tu diagnóstico. Aquí puedes revisar tu resultado por sección, exportarlo en PDF, y cuando estés listo, enviarlo a revisión con el botón Enviar diagnóstico.",
        Routes.ResponderInfoAdicional.route to
            "El administrador te pidió información adicional en algunas preguntas. Contéstalas de nuevo y presiona Reenviar respuestas cuando termines.",
        Routes.DiagnosticDetail.route to
            "Estás viendo el detalle de un diagnóstico. Puedes revisar las fotos, las respuestas de cada sección, el historial de revisiones, y descargar el resumen en PDF.",
        Routes.Notifications.route to
            "Estás en tus notificaciones. Aquí encuentras los avisos sobre tus diagnósticos, por ejemplo cuando un administrador los revisa o te pide más información.",
        Routes.AdminDashboard.route to
            "Estás en el panel de administrador, con un resumen de los diagnósticos por estado y por nivel alcanzado.",
        Routes.AdminUsers.route to
            "Estás en la administración de usuarios. Aquí ves la lista de cuentas de clientes, puedes activarlas o desactivarlas, y dejar comentarios sobre cada una.",
        Routes.AdminQuestions.route to
            "Estás en la gestión de preguntas del diagnóstico. Aquí puedes crear, editar o eliminar los cuestionarios, secciones y preguntas que los clientes contestan al hacer un diagnóstico.",
        Routes.AdminPending.route to
            "Estás en el tablero de diagnósticos pendientes. Aquí ves los diagnósticos organizados por su estado; puedes filtrarlos y entrar a cualquiera para revisarlo.",
        Routes.AdminReview.route to
            "Estás revisando un diagnóstico. Puedes aprobar o rechazar cada pregunta que contestó el cliente, comparar las fotos, pedirle más información si algo falta, y exportar el PDF del cliente o del administrador.",
        Routes.AdminCompare.route to
            "Estás comparando dos diagnósticos lado a lado, para ver las diferencias entre sus resultados."
    )

    private const val TEXTO_POR_DEFECTO =
        "Usa los botones y enlaces de esta pantalla para navegar por la aplicación. Presiona el botón de voz en cualquier momento para escuchar de nuevo estas instrucciones."

    /** [route] es la plantilla de ruta (p.ej. "admin/review/{diagnosticId}"), no la ruta ya resuelta. */
    fun forRoute(route: String?): String = route?.let { porRuta[it] } ?: TEXTO_POR_DEFECTO
}
