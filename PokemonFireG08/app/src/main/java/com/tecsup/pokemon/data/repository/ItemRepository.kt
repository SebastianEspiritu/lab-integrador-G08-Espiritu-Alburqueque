package com.tecsup.pokemon.data.repository

import com.tecsup.pokemon.domain.model.Item
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    // En la implementación futura, estos flujos saldrán de Room.
    val items: Flow<List<Item>>
    val favoritos: Flow<List<Item>>

    // Actualizará Room desde Retrofit y devolverá la cantidad guardada.
    suspend fun refrescar(): Result<Int>
    suspend fun obtenerDetalle(id: String): Result<Item>
    suspend fun alternarFavorito(id: String)
    suspend fun cantidadGuardada(): Int
    suspend fun cantidadFavoritos(): Int
    suspend fun limpiarCache()
}
