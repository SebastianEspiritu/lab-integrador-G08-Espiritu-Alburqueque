package com.tecsup.pokemon.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val imagenUrl: String? = null,
    val tipos: List<String> = emptyList(),
    @Embedded(prefix = "stats_") val estadisticas: EstadisticasEntity? = null,
    val esFavorito: Boolean = false,
    val detalleDisponible: Boolean = false
)

data class EstadisticasEntity(
    val hp: Int,
    val ataque: Int,
    val defensa: Int,
    val ataqueEspecial: Int,
    val defensaEspecial: Int,
    val velocidad: Int
)

class TiposConverter {
    @TypeConverter
    fun guardar(tipos: List<String>): String = Json.encodeToString(tipos)

    @TypeConverter
    fun leer(tipos: String): List<String> = Json.decodeFromString(tipos)
}
