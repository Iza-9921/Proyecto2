package com.example.todoaccesible.data.local.seed

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito

/**
 * Contenido de ejemplo por tipo de inmueble (transcrito 1:1 de
 * `src/data/cuestionariosEjemplo.js` en la web, `EJEMPLOS_POR_TIPO`). Cada
 * tipo arranca con 3 secciones propias de su giro; el admin puede editarlas
 * o agregar más desde el catálogo, igual que en la web. El tipo "Otro" NO
 * está aquí: usa el catálogo fijo de 187 preguntas de [QuestionCatalogSeeder].
 * Un tipo creado por el admin que no aparece en este mapa arranca vacío.
 */
object CuestionarioEjemploSeeder {

    private data class RowEjemplo(val codigo: String, val concepto: String, val credito: Credito, val admiteFoto: Boolean)
    private data class SeccionEjemplo(
        val id: String,
        val icono: String,
        val tituloLargo: String,
        val tituloCorto: String,
        val preguntas: List<RowEjemplo>
    )

    private fun req(codigo: String, concepto: String, foto: Boolean) = RowEjemplo(codigo, concepto, Credito.REQUIRED, foto)
    private fun plus(codigo: String, concepto: String, foto: Boolean) = RowEjemplo(codigo, concepto, Credito.PLUS, foto)

    private val porTipo: Map<String, List<SeccionEjemplo>> = mapOf(
        "Edificio de oficinas" to listOf(
            SeccionEjemplo(
                id = "1", icono = "🕔️", tituloLargo = "Recepción y vestíbulo", tituloCorto = "Recepción",
                preguntas = listOf(
                    req("1.00", "Mostrador de recepción a altura accesible (0.76m - 0.80m)", true),
                    req("1.01", "Área de espera con espacio libre para silla de ruedas", true),
                    req("1.02", "Control de acceso (torniquetes, tarjetas) con carril accesible", true),
                    req("1.03", "Piso antiderrapante en área de recepción", true),
                    plus("1.04", "Directorio en braille y alto relieve", true),
                    plus("1.05", "Iluminación uniforme sin deslumbramiento en el vestíbulo", false)
                )
            ),
            SeccionEjemplo(
                id = "2", icono = "💼", tituloLargo = "Áreas de trabajo y salas de juntas", tituloCorto = "Áreas de trabajo",
                preguntas = listOf(
                    req("2.00", "Espacio de maniobra libre entre escritorios (1.50m)", true),
                    req("2.01", "Sala de juntas con acceso sin desniveles", true),
                    req("2.02", "Rutas de circulación libres de cables y obstáculos", true),
                    plus("2.03", "Escritorio ajustable en altura en al menos un puesto", true),
                    plus("2.04", "Sistema de amplificación de sonido (bucle magnético) en sala de juntas", false)
                )
            ),
            SeccionEjemplo(
                id = "3", icono = "🚻", tituloLargo = "Elevadores y sanitarios", tituloCorto = "Elevadores y sanitarios",
                preguntas = listOf(
                    req("3.00", "Elevador con botonera en braille y a altura accesible", true),
                    req("3.01", "Anuncio audible de piso en el elevador", false),
                    req("3.02", "Sanitario accesible por piso, con barras de apoyo", true),
                    plus("3.03", "Señalización braille en puertas de sanitarios", true),
                    plus("3.04", "Cambiador para adulto en al menos un sanitario", true)
                )
            )
        ),
        "Comercio" to listOf(
            SeccionEjemplo(
                id = "1", icono = "🚪", tituloLargo = "Entrada y fachada", tituloCorto = "Entrada",
                preguntas = listOf(
                    req("1.00", "Entrada principal sin escalones o con rampa alterna", true),
                    req("1.01", "Ancho libre de puerta de entrada (mínimo 0.90m)", true),
                    req("1.02", "Puerta de fácil apertura (automática o de bajo esfuerzo)", true),
                    req("1.03", "Tapete o felpudo empotrado, sin generar desnivel", true),
                    plus("1.04", "Señalización de acceso accesible visible desde la calle", true),
                    plus("1.05", "Timbre o intercomunicador a altura accesible en caso de acceso restringido", false)
                )
            ),
            SeccionEjemplo(
                id = "2", icono = "🛍️", tituloLargo = "Pasillos y exhibición de productos", tituloCorto = "Pasillos y exhibición",
                preguntas = listOf(
                    req("2.00", "Ancho mínimo de pasillos principales (1.20m)", true),
                    req("2.01", "Anaqueles y exhibidores sin obstruir la circulación", true),
                    plus("2.02", "Productos de uso frecuente a altura alcanzable (0.40m - 1.20m)", true),
                    plus("2.03", "Probadores con espacio de maniobra para silla de ruedas", true),
                    req("2.04", "Piso antiderrapante en toda el área de venta", true),
                    plus("2.05", "Señalización de precios con buen contraste y tamaño legible", false)
                )
            ),
            SeccionEjemplo(
                id = "3", icono = "💳", tituloLargo = "Cajas y área de pago", tituloCorto = "Cajas",
                preguntas = listOf(
                    req("3.00", "Al menos una caja con mostrador a altura accesible", true),
                    req("3.01", "Espacio de espera en fila con ancho suficiente para silla de ruedas", true),
                    req("3.02", "Terminal de pago (POS) alcanzable desde silla de ruedas", true),
                    plus("3.03", "Terminal de pago con confirmación audible", false),
                    plus("3.04", "Personal capacitado en atención a personas con discapacidad", false)
                )
            )
        ),
        "Vivienda unifamiliar" to listOf(
            SeccionEjemplo(
                id = "1", icono = "🏠", tituloLargo = "Acceso a la vivienda", tituloCorto = "Acceso",
                preguntas = listOf(
                    req("1.00", "Entrada principal sin escalones o con rampa de pendiente adecuada", true),
                    req("1.01", "Ancho libre de la puerta principal (mínimo 0.90m)", true),
                    req("1.02", "Superficie antiderrapante en el acceso", true),
                    plus("1.03", "Timbre o chapa a altura alcanzable (0.90m - 1.20m)", false),
                    plus("1.04", "Cochera con espacio de transferencia lateral", true)
                )
            ),
            SeccionEjemplo(
                id = "2", icono = "🛋️", tituloLargo = "Interior y circulaciones", tituloCorto = "Interior",
                preguntas = listOf(
                    req("2.00", "Pasillos internos con ancho mínimo de 0.90m", true),
                    req("2.01", "Puertas interiores con ancho libre mínimo de 0.80m", true),
                    req("2.02", "Umbrales sin desniveles entre habitaciones", true),
                    plus("2.03", "Interruptores y contactos a altura alcanzable (0.40m - 1.20m)", true),
                    plus("2.04", "Recámara en planta baja o acceso a nivel superior sin escaleras", false)
                )
            ),
            SeccionEjemplo(
                id = "3", icono = "🚿", tituloLargo = "Baño y cocina accesibles", tituloCorto = "Baño y cocina",
                preguntas = listOf(
                    req("3.00", "Baño con espacio de maniobra de 1.50m de diámetro", true),
                    req("3.01", "Barras de apoyo junto a excusado y regadera", true),
                    plus("3.02", "Regadera sin bordes o con acceso a nivel de piso", true),
                    plus("3.03", "Cocina con espacio libre bajo el fregadero para silla de ruedas", true),
                    plus("3.04", "Muebles de cocina a altura alcanzable", false)
                )
            )
        ),
        "Edificio residencial" to listOf(
            SeccionEjemplo(
                id = "1", icono = "🏢", tituloLargo = "Áreas comunes y accesos", tituloCorto = "Áreas comunes",
                preguntas = listOf(
                    req("1.00", "Acceso peatonal principal sin escalones o con rampa alterna", true),
                    req("1.01", "Vestíbulo de acceso con espacio de maniobra suficiente", true),
                    plus("1.02", "Buzones a altura alcanzable", true),
                    plus("1.03", "Área de paquetería con mostrador accesible", false),
                    req("1.04", "Salón de usos múltiples con acceso sin desniveles", true),
                    plus("1.05", "Alberca o área común con acceso adaptado", true)
                )
            ),
            SeccionEjemplo(
                id = "2", icono = "🛗", tituloLargo = "Elevadores y escaleras", tituloCorto = "Elevadores",
                preguntas = listOf(
                    req("2.00", "Elevador que da servicio a todos los niveles de departamentos", true),
                    req("2.01", "Cabina de elevador con dimensiones mínimas 1.10m x 1.40m", true),
                    req("2.02", "Botonera de elevador en braille y a altura accesible", true),
                    req("2.03", "Escaleras con pasamanos en ambos lados", true),
                    plus("2.04", "Franja antiderrapante y de contraste en cada escalón", true)
                )
            ),
            SeccionEjemplo(
                id = "3", icono = "🅿️", tituloLargo = "Estacionamiento y exteriores", tituloCorto = "Estacionamiento",
                preguntas = listOf(
                    req("3.00", "Cajones de estacionamiento accesibles cerca del acceso", true),
                    req("3.01", "Ruta accesible entre estacionamiento y elevadores", true),
                    plus("3.02", "Áreas verdes y andadores con superficie firme y antiderrapante", true),
                    plus("3.03", "Bancas y mobiliario de descanso en jardines o áreas comunes", false)
                )
            )
        ),
        "Espacio público" to listOf(
            SeccionEjemplo(
                id = "1", icono = "🚶", tituloLargo = "Accesos y andadores", tituloCorto = "Andadores",
                preguntas = listOf(
                    req("1.00", "Andadores con ancho mínimo de 1.20m libre de obstáculos", true),
                    req("1.01", "Superficie firme, antiderrapante y sin grietas", true),
                    req("1.02", "Rampas en cambios de nivel con pendiente adecuada", true),
                    plus("1.03", "Guías podotáctiles en cruces y puntos clave", true),
                    req("1.04", "Cruces peatonales con rebaje de banqueta", true),
                    plus("1.05", "Semáforos con señal sonora para cruce peatonal", false)
                )
            ),
            SeccionEjemplo(
                id = "2", icono = "🪑", tituloLargo = "Mobiliario urbano y descanso", tituloCorto = "Mobiliario urbano",
                preguntas = listOf(
                    plus("2.00", "Bancas de descanso distribuidas a lo largo del recorrido", true),
                    plus("2.01", "Espacio junto a bancas para silla de ruedas", true),
                    plus("2.02", "Bebederos o fuentes a altura accesible", true),
                    req("2.03", "Sanitarios públicos accesibles cercanos", true),
                    plus("2.04", "Sombra o techumbre en áreas de estancia", false)
                )
            ),
            SeccionEjemplo(
                id = "3", icono = "🚨", tituloLargo = "Señalización y seguridad", tituloCorto = "Señalización",
                preguntas = listOf(
                    req("3.00", "Señalización de orientación con buen contraste y pictogramas", true),
                    req("3.01", "Iluminación adecuada en andadores y cruces por la noche", true),
                    req("3.02", "Elementos de protección peatonal (bolardos, barandales) en zonas de riesgo", true),
                    plus("3.03", "Punto de auxilio o vigilancia identificable", false)
                )
            )
        ),
        "Local comercial" to listOf(
            SeccionEjemplo(
                id = "1", icono = "🏪", tituloLargo = "Fachada y acceso", tituloCorto = "Fachada",
                preguntas = listOf(
                    req("1.00", "Acceso a nivel de banqueta, sin escalones", true),
                    req("1.01", "Ancho libre de puerta de entrada (mínimo 0.90m)", true),
                    plus("1.02", "Rótulo del negocio con buen contraste y tamaño legible", true),
                    plus("1.03", "Rampa portátil disponible si hay desnivel en el acceso", true),
                    req("1.04", "Superficie antiderrapante en el acceso", true)
                )
            ),
            SeccionEjemplo(
                id = "2", icono = "🧾", tituloLargo = "Área de atención al cliente", tituloCorto = "Atención al cliente",
                preguntas = listOf(
                    req("2.00", "Espacio de maniobra suficiente dentro del local (1.50m)", true),
                    req("2.01", "Mostrador de atención con tramo a altura accesible", true),
                    req("2.02", "Terminal de pago (POS) alcanzable desde silla de ruedas", true),
                    req("2.03", "Pasillos internos libres de mercancía u obstáculos", true),
                    plus("2.04", "Personal capacitado en atención a personas con discapacidad", false)
                )
            ),
            SeccionEjemplo(
                id = "3", icono = "🚻", tituloLargo = "Sanitario para clientes", tituloCorto = "Sanitario",
                preguntas = listOf(
                    plus("3.00", "Sanitario accesible disponible para clientes", true),
                    plus("3.01", "Barras de apoyo junto al excusado", true),
                    plus("3.02", "Señalización de sanitario accesible visible", false)
                )
            )
        )
    )

    fun tieneEjemplo(tipo: String): Boolean = porTipo.containsKey(tipo)

    fun sectionEntities(tipo: String): List<SectionEntity> {
        val secciones = porTipo[tipo] ?: return emptyList()
        return secciones.mapIndexed { index, s ->
            SectionEntity(id = s.id, tipo = tipo, nombre = s.tituloLargo, orden = index, icono = s.icono, tituloCorto = s.tituloCorto)
        }
    }

    fun questionEntities(tipo: String): List<QuestionEntity> {
        val secciones = porTipo[tipo] ?: return emptyList()
        var globalOrder = 0
        return secciones.flatMap { seccion ->
            seccion.preguntas.map { row ->
                QuestionEntity(
                    codigo = row.codigo,
                    tipo = tipo,
                    seccionId = seccion.id,
                    concepto = row.concepto,
                    credito = row.credito,
                    admiteFoto = row.admiteFoto,
                    orden = globalOrder++
                )
            }
        }
    }
}
