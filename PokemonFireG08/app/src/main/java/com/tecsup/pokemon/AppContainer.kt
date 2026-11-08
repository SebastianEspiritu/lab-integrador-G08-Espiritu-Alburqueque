package com.tecsup.pokemon

import com.tecsup.pokemon.domain.model.Item

/**
 * Contenedor reservado para construir e inyectar las dependencias de la app.
 * Retrofit, Room y Firebase se incorporarán en sus respectivos hitos.
 */
class AppContainer {
    // Fuente temporal para H4; se sustituirá por el repositorio en su hito.
    fun cargarItemsSimulados(): List<Item> = listOf(
        Item(id = "charmander", nombre = "Charmander", tipos = listOf("fire")),
        Item(id = "vulpix", nombre = "Vulpix", tipos = listOf("fire"), esFavorito = true),
        Item(id = "growlithe", nombre = "Growlithe", tipos = listOf("fire")),
        Item(id = "ponyta", nombre = "Ponyta", tipos = listOf("fire")),
        Item(id = "charmeleon", nombre = "Charmeleon", tipos = listOf("fire")),
        Item(id = "charizard", nombre = "Charizard", tipos = listOf("fire", "flying"))
    )
}
