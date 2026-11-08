package com.tecsup.pokemon.data.repository

import com.tecsup.pokemon.data.local.ItemDao
import com.tecsup.pokemon.data.mapper.aEntity
import com.tecsup.pokemon.data.mapper.aModelo
import com.tecsup.pokemon.data.mapper.aModelos
import com.tecsup.pokemon.data.remote.ApiService
import com.tecsup.pokemon.domain.model.Item
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** Room es la única fuente de los modelos publicados; Retrofit solo actualiza Room. */
class ItemRepositoryImpl(
    private val api: ApiService,
    private val dao: ItemDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ItemRepository {
    private val mutex = Mutex()
    override val items = dao.todos().map { lista -> lista.map { it.aModelo() } }
    override val favoritos = dao.favoritos().map { lista -> lista.map { it.aModelo() } }

    override suspend fun refrescar(): Result<Int> = resultado {
        mutex.withLock {
            val referencias = api.listar().aModelos().distinctBy { it.id }
            dao.guardarReferencias(referencias.map { it.aEntity() })
            referencias.size
        }
    }

    override suspend fun obtenerDetalle(id: String): Result<Item> = resultado {
        // Una ficha local no debe esperar una sincronización remota en curso.
        val guardado = dao.porId(id)
        if (guardado?.detalleDisponible == true) return@resultado guardado.aModelo()
        mutex.withLock {
            // Otra solicitud pudo guardar el detalle mientras esperábamos el bloqueo.
            val local = dao.porId(id)
            if (local?.detalleDisponible == true) return@withLock local.aModelo()
            dao.guardarDetalle(api.obtenerDetalle(id).aModelo().aEntity(detalleDisponible = true))
            checkNotNull(dao.porId(id)).aModelo()
        }
    }

    override suspend fun alternarFavorito(id: String) = dao.alternarFavorito(id)
    override suspend fun cantidadGuardada(): Int = dao.contar()
    override suspend fun cantidadFavoritos(): Int = dao.contarFavoritos()
    override suspend fun limpiarCache() {
        mutex.withLock { dao.borrarTodo() }
    }

    private suspend fun <T> resultado(bloque: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("El servidor respondió con error HTTP ${e.code()}.", e))
        } catch (e: IOException) {
            Result.failure(IllegalStateException("No se pudo conectar. Revisa tu conexión a Internet.", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudieron cargar o guardar los datos de Pokémon.", e))
        }
    }
}
