package com.tecsup.pokemon.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tecsup.pokemon.data.mapper.aEntity
import com.tecsup.pokemon.data.remote.ApiService
import com.tecsup.pokemon.data.remote.dto.ItemDto
import com.tecsup.pokemon.data.remote.dto.TipoDto
import com.tecsup.pokemon.data.repository.ItemRepositoryImpl
import com.tecsup.pokemon.domain.model.EstadisticasBasicas
import com.tecsup.pokemon.domain.model.Item
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val item = Item(
        id = "charizard", nombre = "charizard", imagenUrl = "https://example.com/charizard.png",
        tipos = listOf("fire", "flying"),
        estadisticas = EstadisticasBasicas(78, 84, 78, 109, 85, 100)
    )

    @Test fun daoConservaDetalleYFavoritoAlSincronizarReferencias() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.itemDao()
            dao.guardarDetalle(item.aEntity(true))
            dao.alternarFavorito(item.id)
            dao.guardarReferencias(listOf(ItemEntity(item.id, item.nombre, tipos = listOf("fire"))))
            val guardado = requireNotNull(dao.porId(item.id))
            assertEquals(item.imagenUrl, guardado.imagenUrl)
            assertEquals(item.tipos, guardado.tipos)
            assertEquals(109, requireNotNull(guardado.estadisticas).ataqueEspecial)
            assertTrue(guardado.detalleDisponible)
            assertTrue(guardado.esFavorito)
            assertEquals(listOf(item.id), dao.idsFavoritos())
            assertEquals(1, dao.todos().first().size)
            assertEquals(1, dao.favoritos().first().size)
            assertEquals(1, dao.contar())
            assertEquals(1, dao.contarFavoritos())
            dao.alternarFavorito(item.id)
            assertEquals(0, dao.contarFavoritos())
            dao.borrarTodo()
            assertEquals(0, dao.contar())
        } finally { db.close() }
    }

    @Test fun repositorioPersisteFavoritoYDetalleAlCerrarYReabrirBase() = runBlocking {
        val nombrePrueba = "h7-persistence-test.db"
        context.deleteDatabase(nombrePrueba)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, nombrePrueba).build()
        val sinInternet = object : ApiService {
            override suspend fun listar(): TipoDto = throw IOException("sin red")
            override suspend fun obtenerDetalle(nombre: String): ItemDto = throw IOException("sin red")
        }
        try {
            db.itemDao().guardarDetalle(item.aEntity(true))
            ItemRepositoryImpl(sinInternet, db.itemDao()).alternarFavorito(item.id)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, nombrePrueba).build()
            val repo = ItemRepositoryImpl(sinInternet, db.itemDao())
            assertEquals(1, repo.cantidadGuardada())
            assertEquals(1, repo.cantidadFavoritos())
            assertEquals(item.copy(esFavorito = true), repo.items.first().single())
            assertEquals(item.copy(esFavorito = true), repo.obtenerDetalle(item.id).getOrThrow())
            assertTrue(repo.refrescar().isFailure)
            assertTrue(repo.favoritos.first().single().esFavorito)
            repo.limpiarCache()
            assertEquals(0, repo.cantidadGuardada())
            assertEquals(0, repo.cantidadFavoritos())
        } finally {
            db.close()
            context.deleteDatabase(nombrePrueba)
        }
    }

    @Test fun entidadSinDetalleNoInventaEstadisticas() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.itemDao().guardarReferencias(listOf(ItemEntity("vulpix", "vulpix", tipos = listOf("fire"))))
            val guardado = requireNotNull(db.itemDao().porId("vulpix"))
            assertNull(guardado.estadisticas)
            assertNull(guardado.imagenUrl)
            assertFalse(guardado.detalleDisponible)
        } finally { db.close() }
    }
}
