package com.example.todoaccesible.data.local.seed

import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.Credito

/**
 * Catálogo fijo del Scorecard "Todo Accesible v2.6" (8 secciones, 187
 * preguntas), transcrito de las hojas de referencia (v2.6 NB) compartidas
 * por el cliente. Los códigos "X.00" son encabezado de sección (sin crédito,
 * no se insertan como pregunta evaluable).
 *
 * NOTA: la hoja de referencia no marca explícitamente qué preguntas admiten
 * foto; se asume `admiteFoto = true` para todas por defecto (mejor permitir
 * de más que bloquear evidencia). Ajustar caso por caso si el negocio define
 * una lista específica de preguntas sin evidencia fotográfica.
 *
 * NOTA 2: en la sección 4 (Plus) la hoja de origen repite dos números de
 * fila por un artefacto de escaneo; aquí se renumeran 4.15-4.36 de forma
 * secuencial para mantener códigos únicos, conservando el mismo total (22).
 */
object QuestionCatalogSeeder {

    private data class Row(val sufijo: String, val concepto: String, val credito: Credito)

    private val sections = listOf(
        "1" to "Ruta accesible exterior",
        "2" to "Ruta accesible interior",
        "3" to "Elementos de la ruta accesible",
        "4" to "Accesorios | Complementos | Mobiliario",
        "5" to "Funcionamiento en áreas",
        "6" to "Servicios sanitarios",
        "7" to "Recursos humanos y administración",
        "8" to "Protección civil"
    )

    private val section1 = listOf(
        Row("01", "Bahía de ascenso y descenso", Credito.REQUIRED),
        Row("02", "Cajón de estacionamiento (persona con discapacidad), cantidad, dimensiones y señalización", Credito.REQUIRED),
        Row("03", "Ancho mínimo de circulación peatonal libre de obstáculos", Credito.REQUIRED),
        Row("04", "Cambios de dirección en circulación cada 20m", Credito.REQUIRED),
        Row("05", "Superficie de piso / Acabado", Credito.REQUIRED),
        Row("06", "Elementos que sobresalen", Credito.REQUIRED),
        Row("07", "Elementos de protección peatonal (bolardos, macetas, postes, barandales)", Credito.REQUIRED),
        Row("08", "Tope elevado", Credito.REQUIRED),
        Row("09", "Cruce peatonal", Credito.REQUIRED),
        Row("10", "Distribución funcional de pavimento táctil", Credito.REQUIRED),
        Row("11", "Señal de ubicación tacto visual (Mapa, plano o directorio)", Credito.REQUIRED),
        Row("12", "Cajón de estacionamiento para persona con discapacidad adicional +10% de los requeridos", Credito.PLUS),
        Row("13", "Cajón de estacionamiento persona embarazada y/o adulto mayor, al menos 1 por cada 100 cajones estándar", Credito.PLUS),
        Row("14", "Estación de carga para vehículos eléctricos accesible", Credito.PLUS),
        Row("15", "Área de transferencia en cada cajón/estación accesible", Credito.PLUS),
        Row("16", "Circulaciones peatonales marcadas en superficie de niveles de estacionamiento", Credito.PLUS),
        Row("17", "Ancho de circulación peatonal mínima, +30cm", Credito.PLUS),
        Row("18", "Señal visual de tránsito", Credito.PLUS),
        Row("19", "Señalización visual de orientación en pavimento (Wayfinding)", Credito.PLUS),
        Row("20", "Señal de ubicación con dos formatos táctiles (Alto relieve y Braille)", Credito.PLUS),
        Row("21", "Señal de ubicación con formato alterno (audible, código QR)", Credito.PLUS),
        Row("22", "Señalización sensorial", Credito.PLUS)
    )

    private val section2 = listOf(
        Row("01", "Ancho mínimo de circulación principal", Credito.REQUIRED),
        Row("02", "Ancho mínimo de circulación secundaria", Credito.REQUIRED),
        Row("03", "Espacio para cambios de dirección en circulación a cada 20m", Credito.REQUIRED),
        Row("04", "Superficie de piso / Acabado", Credito.REQUIRED),
        Row("05", "Elementos que sobresalen", Credito.REQUIRED),
        Row("06", "Distribución funcional de pavimento táctil", Credito.REQUIRED),
        Row("07", "Señal de ubicación tacto visual espacios de uso común", Credito.REQUIRED),
        Row("08", "Señalización tacto visual espacios de uso común", Credito.REQUIRED),
        Row("09", "Ancho de circulación principal mínima, +30cm", Credito.PLUS),
        Row("10", "Ancho de circulación secundaria mínima, +10cm", Credito.PLUS),
        Row("11", "Espacio para cambios de dirección en circulación constantes", Credito.PLUS),
        Row("12", "Señalización general en dos formatos visuales (Pictograma y Texto)", Credito.PLUS),
        Row("13", "Señalización general en formatos táctiles (Alto relieve y Braille)", Credito.PLUS),
        Row("14", "Señalización general formato alterno (audible, código QR)", Credito.PLUS),
        Row("15", "Señalización sensorial por medio de vegetación aromática", Credito.PLUS),
        Row("16", "Señal de ubicación interactiva (accesible)", Credito.PLUS)
    )

    private val section3 = listOf(
        Row("01", "Ancho libre mínimo en puertas principales (0.90m de paso libre)", Credito.REQUIRED),
        Row("02", "Ancho libre mínimo en puertas secundarias (0.80m de paso libre)", Credito.REQUIRED),
        Row("03", "Aproximación y maniobra en puertas", Credito.REQUIRED),
        Row("04", "Fuerza de operación en puertas al mínimo esfuerzo (20kgF)", Credito.REQUIRED),
        Row("05", "Señalización visual en puertas de vidrio", Credito.REQUIRED),
        Row("06", "Señalización visual en muros de vidrio", Credito.REQUIRED),
        Row("07", "Torniquete accesible ancho libre de paso", Credito.REQUIRED),
        Row("08", "Recepción / Zonas de atención con ancho/alto mínimo accesible", Credito.REQUIRED),
        Row("09", "Rampas ancho libre mínimo", Credito.REQUIRED),
        Row("10", "Rampas pendiente máxima 7%", Credito.REQUIRED),
        Row("11", "Contraste en escaleras", Credito.REQUIRED),
        Row("12", "Escaleras con descanso cada 15 escalones", Credito.REQUIRED),
        Row("13", "Pasamanos con una altura, zona privada", Credito.REQUIRED),
        Row("14", "Pasamanos con dos alturas, zona pública", Credito.REQUIRED),
        Row("15", "Barandales en zona expuesta", Credito.REQUIRED),
        Row("16", "Elevador / Plataforma: dimensiones de cabina (1.60x1.40m)", Credito.REQUIRED),
        Row("17", "Elevador / Plataforma: ancho libre puerta", Credito.REQUIRED),
        Row("18", "Elevador / Plataforma: tiempo de espera en apertura", Credito.REQUIRED),
        Row("19", "Elevador / Plataforma: botonera exterior/interior", Credito.REQUIRED),
        Row("20", "Elevador / Plataforma: indicador sonoro", Credito.REQUIRED),
        Row("21", "Elevador: pasamanos a una altura", Credito.REQUIRED),
        Row("22", "Fitwel/Well: mapa de amenidades accesible", Credito.REQUIRED),
        Row("23", "Fitwel/Well: señalización de bienestar accesible", Credito.REQUIRED),
        Row("24", "Ancho libre en puertas mínimo, +10cm", Credito.PLUS),
        Row("25", "Doble abatimiento en puertas de áreas comunes", Credito.PLUS),
        Row("26", "Puertas de áreas comunes sin mecanismo de cierre de puertas", Credito.PLUS),
        Row("27", "Contraste entre muros y puertas", Credito.PLUS),
        Row("28", "Contraste entre manija y puerta", Credito.PLUS),
        Row("29", "Señalización visual en puertas y muros de vidrio de 50cm o más", Credito.PLUS),
        Row("30", "Recepción / Zonas de atención con ancho/alto mínimo accesible +50cm", Credito.PLUS),
        Row("31", "Rampa ancho libre de 1.50m", Credito.PLUS),
        Row("32", "Rampa con pendiente menor al 7%", Credito.PLUS),
        Row("33", "Pasamanos con dos alturas en zona privada", Credito.PLUS),
        Row("34", "Señalización táctil en pasamanos", Credito.PLUS),
        Row("35", "Elevador / Plataforma: pasamanos a dos alturas", Credito.PLUS),
        Row("36", "Elevador: espejo de piso a techo", Credito.PLUS),
        Row("37", "Elevador / Plataforma: tiempo de espera en apertura de 6 a 9 segundos", Credito.PLUS),
        Row("38", "Elevador: indicadores de acción por voz", Credito.PLUS)
    )

    private val section4 = listOf(
        Row("01", "Manija o jaladera en puertas", Credito.REQUIRED),
        Row("02", "Tiradores tipo jaladera en mobiliario de uso común", Credito.REQUIRED),
        Row("03", "Apagadores: altura", Credito.REQUIRED),
        Row("04", "Termostatos / Control de temperatura: altura máxima de 1.30m", Credito.REQUIRED),
        Row("05", "Contactos: altura", Credito.REQUIRED),
        Row("06", "Lectores de tarjeta: altura", Credito.REQUIRED),
        Row("07", "Botonera código/control de acceso: altura", Credito.REQUIRED),
        Row("08", "Agendas digitales / pantallas interactivas: altura", Credito.REQUIRED),
        Row("09", "Dispositivos alternos: altura y funcionamiento", Credito.REQUIRED),
        Row("10", "Escritorio: dimensiones", Credito.REQUIRED),
        Row("11", "Mesa sala de reunión / comensales: dimensiones", Credito.REQUIRED),
        Row("12", "Zona de atención / Ventanilla: altura", Credito.REQUIRED),
        Row("13", "Bebederos/dispensador de bebidas accesibles", Credito.REQUIRED),
        Row("14", "Cocineta / Pantry para colaboradores", Credito.REQUIRED),
        Row("15", "Mirilla altura accesible en puertas", Credito.PLUS),
        Row("16", "Placas de protección en puertas de madera y metal", Credito.PLUS),
        Row("17", "Accionamientos de puertas alterno (pedal, palanca de pie)", Credito.PLUS),
        Row("18", "Contraste en tapas de contactos, apagadores o dispositivos", Credito.PLUS),
        Row("19", "Sticker con sistema Braille en dispositivos de uso común (vending machine, microondas, etc.)", Credito.PLUS),
        Row("20", "Casilleros: asignación y complementos", Credito.PLUS),
        Row("21", "Botiquín de primeros auxilios, altura", Credito.PLUS),
        Row("22", "Altura y funcionamiento de impresora y/o mueble", Credito.PLUS),
        Row("23", "Altura y funcionamiento de microondas y/o mueble", Credito.PLUS),
        Row("24", "Altura y funcionamiento de máquinas expendedoras", Credito.PLUS),
        Row("25", "Instructivo para dispositivos de uso común", Credito.PLUS),
        Row("26", "Escritorios ajustables en altura", Credito.PLUS),
        Row("27", "Silla ajustable en altura", Credito.PLUS),
        Row("28", "Sillas en áreas individuales con ruedas en los soportes", Credito.PLUS),
        Row("29", "Escalón/escalera plegable para personas de talla baja", Credito.PLUS),
        Row("30", "Báscula para usuarios de silla de ruedas", Credito.PLUS),
        Row("31", "Camilla de inspección ajustable de altura", Credito.PLUS),
        Row("32", "Aparatos de gimnasio para PCD", Credito.PLUS),
        Row("33", "Pizarrón: altura", Credito.PLUS),
        Row("34", "Regletas de comunicación", Credito.PLUS),
        Row("35", "Bucle magnético portátil en zona de mayor afluencia", Credito.PLUS),
        Row("36", "Podio accesible", Credito.PLUS)
    )

    private val section5 = listOf(
        Row("01", "Área de trabajo: maniobra entre escritorios", Credito.REQUIRED),
        Row("02", "Área de impresión: maniobra dentro del espacio", Credito.REQUIRED),
        Row("03", "Recepción / Vestíbulo / Zona de atención / Ventanilla: maniobra frente al elemento", Credito.REQUIRED),
        Row("04", "Sala individual / One to one / Phone Booth / Touchdown: al menos una accesible por zona", Credito.REQUIRED),
        Row("05", "Sala de reunión: maniobra dentro del espacio", Credito.REQUIRED),
        Row("06", "Auditorio o sala de capacitación: acceso a templete", Credito.REQUIRED),
        Row("07", "Auditorio: zona espectador accesible", Credito.REQUIRED),
        Row("08", "Área de comensales: maniobra dentro del espacio", Credito.REQUIRED),
        Row("09", "Área de servicio: mostradores, entrega y recepción de alimentos, entre otros", Credito.REQUIRED),
        Row("10", "Coffee break / Desayunador / Comedor: maniobra dentro del espacio", Credito.REQUIRED),
        Row("11", "Enfermería o centro médico: maniobra dentro del espacio", Credito.REQUIRED),
        Row("12", "Sala de lactancia: maniobra dentro del espacio", Credito.REQUIRED),
        Row("13", "Gimnasio: maniobra dentro del espacio", Credito.PLUS),
        Row("14", "Área de animales de asistencia", Credito.PLUS),
        Row("15", "Sala de bienestar / Wellness / Sala de juegos: maniobra dentro del espacio", Credito.PLUS),
        Row("16", "Quiet Room (cuarto de silencio): maniobra dentro del espacio", Credito.PLUS),
        Row("17", "Sala de oración / Capilla: acceso al espacio", Credito.PLUS),
        Row("18", "Kid's room: maniobra dentro del espacio", Credito.PLUS)
    )

    private val section6 = listOf(
        Row("01", "Acceso a sanitario (general o cubículo)", Credito.REQUIRED),
        Row("02", "Alarma visual al interior del espacio", Credito.REQUIRED),
        Row("03", "Lavamanos en barra general accesible", Credito.REQUIRED),
        Row("04", "Accesorios de lavamanos en barra general (espejo, dispensador jabón o papel, secado de manos)", Credito.REQUIRED),
        Row("05", "Cubículo PCD dimensiones mínimas internas 1.50m por 1.70m", Credito.REQUIRED),
        Row("06", "Sanitario accesible dimensiones mínimas internas 1.80m por 2.50m", Credito.REQUIRED),
        Row("07", "Lavamanos en sanitario accesible", Credito.REQUIRED),
        Row("08", "Accesorios de lavamanos en sanitario (espejo, dispensador jabón o papel, secado de manos)", Credito.REQUIRED),
        Row("09", "Excusado accesible", Credito.REQUIRED),
        Row("10", "Accesorios de excusado (dispensador de papel, asiento, tapa, barras de apoyo 90cm, perchero)", Credito.REQUIRED),
        Row("11", "Mingitorio accesible, altura", Credito.REQUIRED),
        Row("12", "Mingitorio dimensión entre divisiones", Credito.REQUIRED),
        Row("13", "Accesorios de mingitorio (barras de apoyo, perchero)", Credito.REQUIRED),
        Row("14", "Regadera accesible, dimensiones internas 1.50m por 0.90m", Credito.REQUIRED),
        Row("15", "Regadera accesible de tipo teléfono", Credito.REQUIRED),
        Row("16", "Accesorios de regadera (barras, banca, jaboneras, toallero, entre otros)", Credito.REQUIRED),
        Row("17", "Llave de sensor en lavamanos", Credito.PLUS),
        Row("18", "Espejo adicional de cuerpo completo a un costado del lavamanos", Credito.PLUS),
        Row("19", "Dispensadores automáticos de jabón y papel en lavamanos", Credito.PLUS),
        Row("20", "Cubículo PCD dimensiones internas mínimas +5cm en cada lado", Credito.PLUS),
        Row("21", "Asiento para personas de talla baja en excusado (asiento infantil)", Credito.PLUS),
        Row("22", "Regadera PCD dimensiones internas mínimas +5cm en cada lado", Credito.PLUS),
        Row("23", "Regadera tipo teléfono en todos los cubículos", Credito.PLUS),
        Row("24", "Fluxómetros automáticos", Credito.PLUS),
        Row("25", "Percheros en dos alturas (estándar y accesible)", Credito.PLUS),
        Row("26", "Botón de emergencia al interior del cubículo o sanitario accesible", Credito.PLUS),
        Row("27", "Botón de emergencia en regadera accesible", Credito.PLUS),
        Row("28", "Sanitario unisex accesible", Credito.PLUS),
        Row("29", "Sanitario familiar accesible", Credito.PLUS),
        Row("30", "Sanitario sin género", Credito.PLUS),
        Row("31", "Cambiador para adulto", Credito.PLUS),
        Row("32", "Cambiador infantil con altura accesible", Credito.PLUS)
    )

    private val section7 = listOf(
        Row("01", "Política de no discriminación laboral", Credito.REQUIRED),
        Row("02", "Acceso al inmueble a perros de asistencia", Credito.REQUIRED),
        Row("03", "Certificación en igualdad laboral y/o temas de inclusión", Credito.PLUS),
        Row("04", "Certificación Éntrale u homóloga", Credito.PLUS),
        Row("05", "Taller de protección ciudadana con enfoque a personas con discapacidad", Credito.PLUS),
        Row("06", "Taller de concientización con enfoque a personas con discapacidad e inclusión", Credito.PLUS),
        Row("07", "Taller de documentos digitales / Comunicación incluyente y accesible", Credito.PLUS),
        Row("08", "Aplicación interna accesible", Credito.PLUS),
        Row("09", "Certificación W3C (World Wide Web Consortium)", Credito.PLUS),
        Row("10", "Taller de reclutamiento incluyente", Credito.PLUS),
        Row("11", "Contratación de personas con discapacidad", Credito.PLUS)
    )

    private val section8 = listOf(
        Row("01", "Señalización de protección civil según NOM-003", Credito.REQUIRED),
        Row("02", "Salida de emergencia accesible", Credito.REQUIRED),
        Row("03", "Altura de accionamiento de alarmas a máximo 1.20m", Credito.REQUIRED),
        Row("04", "Alarmas visuales en zonas abiertas", Credito.REQUIRED),
        Row("05", "Alarmas auditivas dentro del espacio", Credito.REQUIRED),
        Row("06", "Área de resguardo libre de obstáculos", Credito.REQUIRED),
        Row("07", "Área de resguardo para evacuación cercana a salida de emergencia", Credito.REQUIRED),
        Row("08", "Interfón en área de resguardo PCD", Credito.REQUIRED),
        Row("09", "Protocolo de evacuación interno para PCD", Credito.REQUIRED),
        Row("10", "Visibilidad y alcance de extintores", Credito.REQUIRED),
        Row("11", "Señalización de protección civil con dimensión mayor a 30x30cm", Credito.PLUS),
        Row("12", "Iluminación de emergencia", Credito.PLUS),
        Row("13", "Silla de evacuación", Credito.PLUS),
        Row("14", "Ruta de evacuación fotoluminiscente", Credito.PLUS)
    )

    private val rowsBySection = mapOf(
        "1" to section1, "2" to section2, "3" to section3, "4" to section4,
        "5" to section5, "6" to section6, "7" to section7, "8" to section8
    )

    /** Tipo bajo el cual se siembra este catálogo fijo (equivalente al fallback genérico "Otro" de la web). */
    const val TIPO = "Otro"

    fun sectionEntities(tipo: String = TIPO): List<SectionEntity> =
        sections.mapIndexed { index, (id, nombre) -> SectionEntity(id = id, tipo = tipo, nombre = nombre, orden = index) }

    fun questionEntities(tipo: String = TIPO): List<QuestionEntity> {
        var globalOrder = 0
        return sections.flatMap { (sectionId, _) ->
            rowsBySection.getValue(sectionId).map { row ->
                QuestionEntity(
                    codigo = "$sectionId.${row.sufijo}",
                    tipo = tipo,
                    seccionId = sectionId,
                    concepto = row.concepto,
                    credito = row.credito,
                    admiteFoto = true,
                    orden = globalOrder++
                )
            }
        }
    }
}
