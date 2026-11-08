package com.tecsup.pokemon.data.repository

import com.tecsup.pokemon.data.mapper.aModelo
import com.tecsup.pokemon.data.mapper.aModelos
import com.tecsup.pokemon.data.remote.ApiService
import com.tecsup.pokemon.domain.model.Item
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/** H6: caché y favoritos SOLO en memoria; se pierden al terminar el proceso. */
class ItemRepositoryImpl(private val api: ApiService) : ItemRepository {
    private val memoria = MutableStateFlow<List<Item>>(emptyList())
    private val detalles = mutableMapOf<String, Item>()
    private val mutex = Mutex()
    override val items = memoria.asStateFlow()
    override val favoritos = items.map { lista -> lista.filter { it.esFavorito } }

    override suspend fun refrescar(): Result<Int> = resultado {
        mutex.withLock {
            // Una petición de lista, sin descargar un detalle por cada Pokémon.
            val referencias = api.listar().aModelos().distinctBy { it.id }
            val anteriores = memoria.value.associateBy { it.id }
            memoria.value = referencias.map { referencia ->
                (detalles[referencia.id] ?: referencia).copy(
                    esFavorito = anteriores[referencia.id]?.esFavorito ?: false
                )
            }
            memoria.value.size
        }
    }

    override suspend fun obtenerDetalle(id: String): Result<Item> = resultado {
        mutex.withLock {
            val favorito = memoria.value.firstOrNull { it.id == id }?.esFavorito ?: false
            val detalle = (detalles[id] ?: api.obtenerDetalle(id).aModelo()).copy(esFavorito = favorito)
            detalles[id] = detalle
            memoria.update { lista -> lista.map { if (it.id == id) detalle else it } }
            detalle
        }
    }

    override suspend fun alternarFavorito(id: String) {
        mutex.withLock {
            memoria.update { lista -> lista.map { if (it.id == id) it.copy(esFavorito = !it.esFavorito) else it } }
        }
    }

    override suspend fun cantidadGuardada(): Int = memoria.value.size
    override suspend fun cantidadFavoritos(): Int = memoria.value.count { it.esFavorito }
    override suspend fun limpiarCache() {
        mutex.withLock {
            detalles.clear()
            memoria.value = emptyList()
        }
    }

    // Traduce fallos de infraestructura: la UI no importa Retrofit.
    private suspend fun <T> resultado(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(IllegalStateException("El servidor respondió con error HTTP ${e.code()}.", e))
    } catch (e: IOException) {
        Result.failure(IllegalStateException("No se pudo conectar. Revisa tu conexión a Internet.", e))
    } catch (e: Exception) {
        Result.failure(IllegalStateException("No se pudieron interpretar los datos de Pokémon.", e))
    }
}
