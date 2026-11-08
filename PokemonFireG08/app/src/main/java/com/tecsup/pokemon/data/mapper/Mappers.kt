package com.tecsup.pokemon.data.mapper

import com.tecsup.pokemon.data.remote.dto.ItemDto
import com.tecsup.pokemon.data.remote.dto.TipoDto
import com.tecsup.pokemon.domain.model.EstadisticasBasicas
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.data.local.ItemEntity
import com.tecsup.pokemon.data.local.EstadisticasEntity

/** Solo se conoce el tipo consultado; los demás tipos requieren el detalle. */
fun TipoDto.aModelos(): List<Item> = pokemon.map { entrada ->
    Item(
        id = entrada.pokemon.name,
        nombre = entrada.pokemon.name,
        tipos = listOf(name)
    )
}

fun Item.aEntity(detalleDisponible: Boolean = false): ItemEntity = ItemEntity(
    id = id, nombre = nombre, imagenUrl = imagenUrl, tipos = tipos,
    estadisticas = estadisticas?.let {
        EstadisticasEntity(it.hp, it.ataque, it.defensa, it.ataqueEspecial, it.defensaEspecial, it.velocidad)
    },
    esFavorito = esFavorito, detalleDisponible = detalleDisponible
)

fun ItemEntity.aModelo(): Item = Item(
    id = id, nombre = nombre, imagenUrl = imagenUrl, tipos = tipos,
    estadisticas = estadisticas?.let {
        EstadisticasBasicas(it.hp, it.ataque, it.defensa, it.ataqueEspecial, it.defensaEspecial, it.velocidad)
    },
    esFavorito = esFavorito
)

fun ItemDto.aModelo(esFavorito: Boolean = false): Item {
    val valores = stats.associate { it.stat.name to it.baseStat }
    val requeridas = listOf("hp", "attack", "defense", "special-attack", "special-defense", "speed")
    // No inventar estadísticas si una respuesta está incompleta.
    val estadisticas = if (requeridas.all { it in valores }) {
        EstadisticasBasicas(
            hp = valores.getValue("hp"),
            ataque = valores.getValue("attack"),
            defensa = valores.getValue("defense"),
            ataqueEspecial = valores.getValue("special-attack"),
            defensaEspecial = valores.getValue("special-defense"),
            velocidad = valores.getValue("speed")
        )
    } else null

    return Item(
        id = name,
        nombre = name,
        imagenUrl = sprites?.other?.officialArtwork?.frontDefault ?: sprites?.frontDefault,
        tipos = types.sortedBy { it.slot }.map { it.type.name },
        estadisticas = estadisticas,
        esFavorito = esFavorito
    )
}
