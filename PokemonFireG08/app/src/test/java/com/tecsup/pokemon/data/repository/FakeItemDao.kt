package com.tecsup.pokemon.data.repository

import com.tecsup.pokemon.data.local.ItemDao
import com.tecsup.pokemon.data.local.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Doble de almacenamiento para probar sin Android; no sustituye las pruebas Room. */
class FakeItemDao : ItemDao() {
    private val filas = MutableStateFlow<List<ItemEntity>>(emptyList())
    override fun todos() = filas.map { it.sortedBy { fila -> fila.nombre } }
    override fun favoritos() = todos().map { lista -> lista.filter { it.esFavorito } }
    override suspend fun porId(id: String) = filas.value.firstOrNull { it.id == id }
    override suspend fun todosActuales() = filas.value
    override suspend fun idsFavoritos() = filas.value.filter { it.esFavorito }.map { it.id }
    override suspend fun contar() = filas.value.size
    override suspend fun contarFavoritos() = filas.value.count { it.esFavorito }
    override suspend fun guardarTodos(items: List<ItemEntity>) {
        filas.update { anteriores -> (anteriores.associateBy { it.id } + items.associateBy { it.id }).values.toList() }
    }
    override suspend fun alternarFavorito(id: String) {
        filas.update { lista -> lista.map { if (it.id == id) it.copy(esFavorito = !it.esFavorito) else it } }
    }
    override suspend fun borrarTodo() { filas.value = emptyList() }
}
