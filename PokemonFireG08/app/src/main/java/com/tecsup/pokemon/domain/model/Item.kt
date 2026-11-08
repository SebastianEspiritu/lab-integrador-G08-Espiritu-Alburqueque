package com.tecsup.pokemon.domain.model

/** Modelo de dominio independiente de la API, Room y Compose. */
data class Item(
    // Nombre usado como identificador en /pokemon/{nombre}.
    val id: String,
    val nombre: String,
    val imagenUrl: String? = null,
    val tipos: List<String> = emptyList(),
    // Puede no estar disponible hasta consultar el detalle.
    val estadisticas: EstadisticasBasicas? = null,
    val esFavorito: Boolean = false
)

data class EstadisticasBasicas(
    val hp: Int,
    val ataque: Int,
    val defensa: Int,
    val ataqueEspecial: Int,
    val defensaEspecial: Int,
    val velocidad: Int
)
