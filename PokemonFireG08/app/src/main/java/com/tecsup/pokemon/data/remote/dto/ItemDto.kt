package com.tecsup.pokemon.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** /type/fire contiene referencias, no detalles ni sprites. */
@Serializable
data class TipoDto(val name: String, val pokemon: List<PokemonPorTipoDto>)

@Serializable
data class PokemonPorTipoDto(val slot: Int, val pokemon: RecursoDto)

@Serializable
data class RecursoDto(val name: String, val url: String)

/** Respuesta de /pokemon/{nombre}; solo los campos necesarios para el dominio. */
@Serializable
data class ItemDto(
    val name: String,
    val types: List<TipoPokemonDto>,
    val stats: List<EstadisticaDto>,
    val sprites: SpritesDto? = null
)

@Serializable
data class TipoPokemonDto(val slot: Int, val type: RecursoDto)

@Serializable
data class EstadisticaDto(
    @SerialName("base_stat") val baseStat: Int,
    val stat: RecursoDto
)

@Serializable
data class SpritesDto(
    @SerialName("front_default") val frontDefault: String? = null,
    val other: OtrosSpritesDto? = null
)

@Serializable
data class OtrosSpritesDto(
    @SerialName("official-artwork") val officialArtwork: IlustracionDto? = null
)

@Serializable
data class IlustracionDto(
    @SerialName("front_default") val frontDefault: String? = null
)
