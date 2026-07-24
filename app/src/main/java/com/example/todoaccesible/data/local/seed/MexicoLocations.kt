package com.example.todoaccesible.data.local.seed

/**
 * Catálogo fijo de estados de México y ciudades/municipios representativos
 * por estado, usado en el selector "Estado / Ciudad" del registro del
 * proyecto. No es exhaustivo (no cubre todos los municipios), pero cubre las
 * ciudades principales de cada entidad.
 */
object MexicoLocations {

    val ciudadesPorEstado: Map<String, List<String>> = linkedMapOf(
        "Aguascalientes" to listOf("Aguascalientes", "Jesús María", "Calvillo"),
        "Baja California" to listOf("Tijuana", "Mexicali", "Ensenada", "Rosarito"),
        "Baja California Sur" to listOf("La Paz", "Los Cabos", "Comondú"),
        "Campeche" to listOf("Campeche", "Ciudad del Carmen", "Champotón"),
        "Chiapas" to listOf("Tuxtla Gutiérrez", "San Cristóbal de las Casas", "Tapachula"),
        "Chihuahua" to listOf("Chihuahua", "Ciudad Juárez", "Delicias", "Cuauhtémoc"),
        "Ciudad de México" to listOf(
            "Álvaro Obregón", "Azcapotzalco", "Benito Juárez", "Coyoacán", "Cuauhtémoc",
            "Gustavo A. Madero", "Iztapalapa", "Miguel Hidalgo", "Tlalpan", "Xochimilco"
        ),
        "Coahuila" to listOf("Saltillo", "Torreón", "Monclova", "Piedras Negras"),
        "Colima" to listOf("Colima", "Manzanillo", "Tecomán"),
        "Durango" to listOf("Durango", "Gómez Palacio", "Lerdo"),
        "Estado de México" to listOf(
            "Toluca", "Naucalpan", "Ecatepec", "Tlalnepantla", "Nezahualcóyotl", "Metepec"
        ),
        "Guanajuato" to listOf("León", "Guanajuato", "Irapuato", "Celaya", "Salamanca"),
        "Guerrero" to listOf("Chilpancingo", "Acapulco", "Iguala", "Taxco"),
        "Hidalgo" to listOf("Pachuca", "Tulancingo", "Tizayuca"),
        "Jalisco" to listOf("Guadalajara", "Zapopan", "Tlaquepaque", "Puerto Vallarta"),
        "Michoacán" to listOf("Morelia", "Uruapan", "Zamora", "Pátzcuaro"),
        "Morelos" to listOf("Cuernavaca", "Jiutepec", "Emiliano Zapata", "Temixco", "Cuautla", "Yautepec"),
        "Nayarit" to listOf("Tepic", "Bahía de Banderas", "Santiago Ixcuintla"),
        "Nuevo León" to listOf("Monterrey", "San Pedro Garza García", "Guadalupe", "San Nicolás de los Garza"),
        "Oaxaca" to listOf("Oaxaca de Juárez", "Salina Cruz", "Tuxtepec"),
        "Puebla" to listOf("Puebla", "Tehuacán", "Cholula", "Atlixco"),
        "Querétaro" to listOf("Querétaro", "San Juan del Río", "Corregidora"),
        "Quintana Roo" to listOf("Cancún", "Playa del Carmen", "Chetumal", "Tulum"),
        "San Luis Potosí" to listOf("San Luis Potosí", "Soledad de Graciano Sánchez", "Ciudad Valles"),
        "Sinaloa" to listOf("Culiacán", "Mazatlán", "Los Mochis"),
        "Sonora" to listOf("Hermosillo", "Ciudad Obregón", "Nogales", "San Luis Río Colorado"),
        "Tabasco" to listOf("Villahermosa", "Cárdenas", "Comalcalco"),
        "Tamaulipas" to listOf("Ciudad Victoria", "Reynosa", "Matamoros", "Nuevo Laredo", "Tampico"),
        "Tlaxcala" to listOf("Tlaxcala", "Apizaco", "Huamantla"),
        "Veracruz" to listOf("Xalapa", "Veracruz", "Coatzacoalcos", "Córdoba", "Orizaba"),
        "Yucatán" to listOf("Mérida", "Valladolid", "Progreso"),
        "Zacatecas" to listOf("Zacatecas", "Fresnillo", "Guadalupe")
    )

    val estados: List<String> = ciudadesPorEstado.keys.toList()

    fun ciudadesDe(estado: String): List<String> = ciudadesPorEstado[estado].orEmpty()
}
