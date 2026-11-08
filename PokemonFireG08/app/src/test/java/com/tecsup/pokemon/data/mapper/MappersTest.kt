package com.tecsup.pokemon.data.mapper

import com.tecsup.pokemon.data.remote.dto.ItemDto
import com.tecsup.pokemon.data.remote.dto.TipoDto
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MappersTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun listaInterpretaReferenciasSinInventarDetalle() {
        val dto = json.decodeFromString<TipoDto>(
            """{
                "name":"fire", "id":10,
                "pokemon":[{"slot":1,"pokemon":{"name":"charmander","url":"https://pokeapi.co/api/v2/pokemon/4/"}}]
            }"""
        )
        val item = dto.aModelos().single()
        assertEquals("charmander", item.id)
        assertEquals("charmander", item.nombre)
        assertEquals(listOf("fire"), item.tipos)
        assertNull(item.imagenUrl)
        assertNull(item.estadisticas)
        assertFalse(item.esFavorito)
    }

    @Test
    fun detalleMapeaEstadisticasPorNombreYTiposPorSlot() {
        val item = json.decodeFromString<ItemDto>(detalle).aModelo(esFavorito = true)
        assertEquals("charizard", item.id)
        assertEquals("charizard", item.nombre)
        assertEquals(listOf("fire", "flying"), item.tipos)
        assertEquals("https://example.com/art.png", item.imagenUrl)
        assertTrue(item.esFavorito)
        val stats = requireNotNull(item.estadisticas)
        assertEquals(78, stats.hp)
        assertEquals(84, stats.ataque)
        assertEquals(78, stats.defensa)
        assertEquals(109, stats.ataqueEspecial)
        assertEquals(85, stats.defensaEspecial)
        assertEquals(100, stats.velocidad)
    }

    @Test
    fun detalleUsaSpriteSiNoHayIlustracionOficial() {
        val dto = json.decodeFromString<ItemDto>(
            detalle.replace("\"front_default\":\"https://example.com/art.png\"", "\"front_default\":null")
        )
        assertEquals("https://example.com/sprite.png", dto.aModelo().imagenUrl)
    }

    @Test
    fun detalleToleraSpritesNulosYEstadisticasIncompletas() {
        val dto = json.decodeFromString<ItemDto>(
            """{"name":"vulpix","types":[],"stats":[],"sprites":null}"""
        )
        val item = dto.aModelo()
        assertNull(item.imagenUrl)
        assertNull(item.estadisticas)
        assertFalse(item.esFavorito)
    }

    @Test
    fun listaVaciaProduceListaVacia() {
        assertTrue(json.decodeFromString<TipoDto>("""{"name":"fire","pokemon":[]}""").aModelos().isEmpty())
    }

    private val detalle = """{
        "id":6,"name":"charizard","height":17,
        "types":[
            {"slot":2,"type":{"name":"flying","url":"https://pokeapi.co/api/v2/type/3/"}},
            {"slot":1,"type":{"name":"fire","url":"https://pokeapi.co/api/v2/type/10/"}}
        ],
        "stats":[
            {"base_stat":100,"stat":{"name":"speed","url":"https://pokeapi.co/api/v2/stat/6/"}},
            {"base_stat":78,"stat":{"name":"hp","url":"https://pokeapi.co/api/v2/stat/1/"}},
            {"base_stat":85,"stat":{"name":"special-defense","url":"https://pokeapi.co/api/v2/stat/5/"}},
            {"base_stat":84,"stat":{"name":"attack","url":"https://pokeapi.co/api/v2/stat/2/"}},
            {"base_stat":109,"stat":{"name":"special-attack","url":"https://pokeapi.co/api/v2/stat/4/"}},
            {"base_stat":78,"stat":{"name":"defense","url":"https://pokeapi.co/api/v2/stat/3/"}}
        ],
        "sprites":{
            "front_default":"https://example.com/sprite.png",
            "other":{"official-artwork":{"front_default":"https://example.com/art.png"}}
        }
    }"""
}
