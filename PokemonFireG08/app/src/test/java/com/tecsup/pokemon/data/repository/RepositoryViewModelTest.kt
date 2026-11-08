package com.tecsup.pokemon.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.tecsup.pokemon.data.remote.ApiService
import com.tecsup.pokemon.data.remote.dto.*
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.detalle.DetalleViewModel
import com.tecsup.pokemon.ui.lista.ListaViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @Before fun configurarMain() { Dispatchers.setMain(dispatcher) }
    @After fun limpiar() { store.clear(); Dispatchers.resetMain() }
    private fun <T : ViewModel> retener(key: String, vm: T): T = vm.also { store.put(key, it) }

    @Test fun listaCargaReferenciasSinPedirDetalles() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        val vm = retener("lista", ListaViewModel(repo))
        assertEquals(UiState.Cargando, vm.estado.value)
        advanceUntilIdle()
        val lista = (vm.estado.value as UiState.Exito).datos
        assertEquals("charmander", lista.single().id)
        assertNull(lista.single().estadisticas)
        assertEquals(1, api.llamadasLista)
        assertEquals(0, api.llamadasDetalle)
    }

    @Test fun listaVaciaEsExitoYNoQuedaCargando() = runTest(dispatcher) {
        val api = FakeApi().apply { vacia = true }
        val vm = retener("lista", ListaViewModel(ItemRepositoryImpl(api, FakeItemDao(), dispatcher)))
        advanceUntilIdle()
        assertEquals(UiState.Exito(emptyList<Any>()), vm.estado.value)
    }

    @Test fun listaMuestraErrorYPermiteReintentar() = runTest(dispatcher) {
        val api = FakeApi().apply { fallo = IOException("sin red") }
        val vm = retener("lista", ListaViewModel(ItemRepositoryImpl(api, FakeItemDao(), dispatcher)))
        advanceUntilIdle()
        assertTrue((vm.estado.value as UiState.Error).mensaje.contains("conexión"))
        api.fallo = null
        vm.cargar()
        vm.cargar() // El segundo toque no duplica la petición en curso.
        advanceUntilIdle()
        assertTrue(vm.estado.value is UiState.Exito)
        assertEquals(2, api.llamadasLista)
    }

    @Test fun detalleActualizaListaYReutilizaAlmacenamientoLocal() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        val lista = retener("lista", ListaViewModel(repo))
        advanceUntilIdle()
        val detalle = retener("detalle", DetalleViewModel(repo, "charmander"))
        advanceUntilIdle()
        val item = (detalle.estado.value as UiState.Exito).datos
        assertEquals("https://example.com/charmander.png", item.imagenUrl)
        assertNull(item.estadisticas) // La fake no proporciona estadísticas.
        assertEquals(item, (lista.estado.value as UiState.Exito).datos.single())
        assertTrue(repo.obtenerDetalle("charmander").isSuccess)
        assertEquals(1, api.llamadasDetalle)
    }

    @Test fun detalleMuestraHttp403YReintenta() = runTest(dispatcher) {
        val api = FakeApi().apply {
            fallo = HttpException(Response.error<Any>(403, "Forbidden".toResponseBody()))
        }
        val vm = retener("detalle", DetalleViewModel(ItemRepositoryImpl(api, FakeItemDao(), dispatcher), "charmander"))
        advanceUntilIdle()
        assertTrue((vm.estado.value as UiState.Error).mensaje.contains("403"))
        api.fallo = null
        vm.cargar()
        advanceUntilIdle()
        assertTrue(vm.estado.value is UiState.Exito)
    }

    @Test fun favoritosSeConservanDuranteRefrescoYAlRecrearRepositorio() = runTest(dispatcher) {
        val api = FakeApi()
        val dao = FakeItemDao()
        val repo = ItemRepositoryImpl(api, dao, dispatcher)
        repo.refrescar().getOrThrow()
        repo.alternarFavorito("charmander")
        repo.obtenerDetalle("charmander").getOrThrow()
        repo.refrescar().getOrThrow()
        assertEquals(1, repo.cantidadFavoritos())
        assertEquals(1, repo.favoritos.first().size)
        assertTrue(repo.items.first().single().esFavorito)
        val otraInstancia = ItemRepositoryImpl(api, dao, dispatcher)
        assertEquals(1, otraInstancia.cantidadGuardada())
        assertEquals(1, otraInstancia.cantidadFavoritos())
        repo.limpiarCache()
        assertEquals(0, repo.cantidadGuardada())
        assertEquals(0, repo.cantidadFavoritos())
    }

    @Test fun cancelacionSePropagaSinConvertirseEnError() = runTest(dispatcher) {
        val api = FakeApi().apply { fallo = CancellationException("cancelada") }
        try {
            ItemRepositoryImpl(api, FakeItemDao(), dispatcher).refrescar()
            fail("Debe propagar cancelación")
        } catch (_: CancellationException) { /* esperado */ }
    }

    @Test fun listaMuestraLocalesSiNoHayInternetYRecuperaSincronizacion() = runTest(dispatcher) {
        val dao = FakeItemDao()
        val api = FakeApi()
        ItemRepositoryImpl(api, dao, dispatcher).refrescar().getOrThrow()
        api.fallo = IOException("sin red")
        val vm = retener("offline", ListaViewModel(ItemRepositoryImpl(api, dao, dispatcher)))
        advanceUntilIdle()
        assertEquals("charmander", (vm.estado.value as UiState.Exito).datos.single().id)
        assertTrue(requireNotNull(vm.aviso.value).contains("datos guardados"))
        api.fallo = null
        vm.cargar()
        advanceUntilIdle()
        assertNull(vm.aviso.value)
        assertTrue(vm.estado.value is UiState.Exito)
    }

    @Test fun detalleGuardadoSeAbreSinInternetTrasRecrearRepositorio() = runTest(dispatcher) {
        val dao = FakeItemDao()
        val api = FakeApi()
        val primero = ItemRepositoryImpl(api, dao, dispatcher)
        primero.refrescar().getOrThrow()
        val esperado = primero.obtenerDetalle("charmander").getOrThrow()
        api.fallo = IOException("sin red")
        val segundo = ItemRepositoryImpl(api, dao, dispatcher)
        assertEquals(esperado, segundo.obtenerDetalle("charmander").getOrThrow())
        assertEquals(1, api.llamadasDetalle)
    }

    private class FakeApi : ApiService {
        var fallo: Exception? = null
        var vacia = false
        var llamadasLista = 0
        var llamadasDetalle = 0
        override suspend fun listar(): TipoDto {
            llamadasLista++
            fallo?.let { throw it }
            return TipoDto("fire", if (vacia) emptyList() else listOf(
                PokemonPorTipoDto(1, RecursoDto("charmander", "https://pokeapi.co/api/v2/pokemon/4/"))
            ))
        }
        override suspend fun obtenerDetalle(nombre: String): ItemDto {
            llamadasDetalle++
            fallo?.let { throw it }
            return ItemDto(
                name = nombre,
                types = listOf(TipoPokemonDto(1, RecursoDto("fire", "https://pokeapi.co/api/v2/type/10/"))),
                stats = emptyList(),
                sprites = SpritesDto(frontDefault = "https://example.com/charmander.png")
            )
        }
    }
}
