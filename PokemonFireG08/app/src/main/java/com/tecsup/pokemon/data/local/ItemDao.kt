package com.tecsup.pokemon.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ItemDao {
    @Query("SELECT * FROM items ORDER BY nombre")
    abstract fun todos(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE esFavorito = 1 ORDER BY nombre")
    abstract fun favoritos(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    abstract suspend fun porId(id: String): ItemEntity?

    @Query("SELECT * FROM items ORDER BY nombre")
    abstract suspend fun todosActuales(): List<ItemEntity>

    @Query("SELECT id FROM items WHERE esFavorito = 1")
    abstract suspend fun idsFavoritos(): List<String>

    @Query("SELECT COUNT(*) FROM items")
    abstract suspend fun contar(): Int

    @Query("SELECT COUNT(*) FROM items WHERE esFavorito = 1")
    abstract suspend fun contarFavoritos(): Int

    @Upsert
    abstract suspend fun guardarTodos(items: List<ItemEntity>)

    @Query("UPDATE items SET esFavorito = NOT esFavorito WHERE id = :id")
    abstract suspend fun alternarFavorito(id: String)

    @Query("DELETE FROM items")
    abstract suspend fun borrarTodo()

    /** Las referencias no deben borrar detalles ni favoritos ya descargados. */
    @Transaction
    open suspend fun guardarReferencias(items: List<ItemEntity>) {
        val anteriores = todosActuales().associateBy { it.id }
        guardarTodos(items.map { nuevo -> anteriores[nuevo.id]?.copy(nombre = nuevo.nombre) ?: nuevo })
    }

    @Transaction
    open suspend fun guardarDetalle(item: ItemEntity) {
        val anterior = porId(item.id)
        guardarTodos(listOf(item.copy(
            esFavorito = anterior?.esFavorito ?: item.esFavorito,
            imagenUrl = item.imagenUrl ?: anterior?.imagenUrl,
            tipos = item.tipos.ifEmpty { anterior?.tipos ?: emptyList() },
            estadisticas = item.estadisticas ?: anterior?.estadisticas,
            detalleDisponible = true
        )))
    }
}
