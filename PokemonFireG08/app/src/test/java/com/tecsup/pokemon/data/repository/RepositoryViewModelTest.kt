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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
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

    @Test fun detalleLocalNoEsperaUnaSincronizacionRemotaLenta() = runTest(dispatcher) {
        val dao = FakeItemDao()
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, dao, dispatcher)
        val esperado = repo.obtenerDetalle("charmander").getOrThrow()
        val barrera = CompletableDeferred<Unit>()
        api.bloqueoLista = barrera
        val sincronizacion = async { repo.refrescar() }
        runCurrent()
        val detalle = async { repo.obtenerDetalle("charmander") }
        runCurrent()
        try {
            assertTrue("El detalle local no debe esperar la red", detalle.isCompleted)
            assertEquals(esperado, detalle.await().getOrThrow())
        } finally {
            barrera.complete(Unit)
            sincronizacion.await()
        }
    }

    @Test fun sincronizarReferenciasDuplicadasNoDuplicaFilas() = runTest(dispatcher) {
        val api = FakeApi().apply { duplicados = true }
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        assertEquals(1, repo.refrescar().getOrThrow())
        repo.refrescar().getOrThrow()
        assertEquals(1, repo.cantidadGuardada())
    }

    @Test fun errorHttpNoBorraDetalleNiFavoritosGuardados() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        repo.refrescar().getOrThrow()
        repo.alternarFavorito("charmander")
        val esperado = repo.obtenerDetalle("charmander").getOrThrow()
        api.fallo = HttpException(Response.error<Any>(503, "Unavailable".toResponseBody()))
        assertTrue(requireNotNull(repo.refrescar().exceptionOrNull()).message!!.contains("503"))
        assertEquals(esperado, repo.items.first().single())
        assertEquals(1, repo.cantidadFavoritos())
    }

    @Test fun solicitudesConcurrentesDelMismoDetalleHacenUnaPeticion() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        val barrera = CompletableDeferred<Unit>()
        api.bloqueoDetalle = barrera
        val primero = async { repo.obtenerDetalle("charmander") }
        val segundo = async { repo.obtenerDetalle("charmander") }
        runCurrent()
        assertEquals(1, api.llamadasDetalle)
        barrera.complete(Unit)
        assertEquals(primero.await().getOrThrow(), segundo.await().getOrThrow())
        assertEquals(1, api.llamadasDetalle)
    }

    @Test fun limpiarCacheEliminaDetalleYPermiteDescargarDeNuevo() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        repo.obtenerDetalle("charmander").getOrThrow()
        repo.alternarFavorito("charmander")
        repo.limpiarCache()
        assertTrue(repo.items.first().isEmpty())
        assertTrue(repo.favoritos.first().isEmpty())
        assertFalse(repo.obtenerDetalle("charmander").getOrThrow().esFavorito)
        assertEquals(2, api.llamadasDetalle)
    }

    @Test fun filtrarRepetidamenteNoDisparaPeticionesRealesDelContratoApi() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = retener("lista", ListaViewModel(ItemRepositoryImpl(api, FakeItemDao(), dispatcher)))
        advanceUntilIdle()
        repeat(50) {
            vm.buscar("char".take(it % 5))
            vm.seleccionarTipo("fire")
            vm.restablecerFiltros()
        }
        advanceUntilIdle()
        assertEquals(1, api.llamadasLista)
        assertEquals(0, api.llamadasDetalle)
        assertEquals("charmander", (vm.estado.value as UiState.Exito).datos.single().id)
    }

    @Test fun flujosContadoresYFavoritosNoEsperanLaRed() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        val original = repo.obtenerDetalle("charmander").getOrThrow()
        val barrera = CompletableDeferred<Unit>()
        api.bloqueoLista = barrera
        val sincronizacion = async { repo.refrescar() }
        runCurrent()
        val lectura = async {
            repo.alternarFavorito("charmander")
            assertEquals(1, repo.cantidadGuardada())
            assertEquals(1, repo.cantidadFavoritos())
            assertEquals(original.copy(esFavorito = true), repo.items.first().single())
            assertEquals(1, repo.favoritos.first().size)
        }
        runCurrent()
        try {
            assertTrue("Los datos locales no deben esperar la red", lectura.isCompleted)
            lectura.await()
        } finally { barrera.complete(Unit); sincronizacion.await() }
        assertTrue(repo.items.first().single().esFavorito)
    }

    @Test fun cancelarSincronizacionNoBorraCacheYPermiteReintento() = runTest(dispatcher) {
        val api = FakeApi()
        val repo = ItemRepositoryImpl(api, FakeItemDao(), dispatcher)
        repo.obtenerDetalle("charmander").getOrThrow()
        repo.alternarFavorito("charmander")
        val anterior = repo.items.first()
        api.bloqueoLista = CompletableDeferred()
        val sincronizacion = async { repo.refrescar() }
        runCurrent()
        sincronizacion.cancelAndJoin()
        assertEquals(anterior, repo.items.first())
        api.bloqueoLista = null
        assertEquals(1, repo.refrescar().getOrThrow())
        assertEquals(anterior, repo.items.first())
    }

    private class FakeApi : ApiService {
        var fallo: Exception? = null
        var vacia = false
        var llamadasLista = 0
        var llamadasDetalle = 0
        var bloqueoLista: CompletableDeferred<Unit>? = null
        var bloqueoDetalle: CompletableDeferred<Unit>? = null
        var duplicados = false
        override suspend fun listar(): TipoDto {
            llamadasLista++
            bloqueoLista?.await()
            fallo?.let { throw it }
            val referencias = if (vacia) emptyList() else listOf(
                PokemonPorTipoDto(1, RecursoDto("charmander", "https://pokeapi.co/api/v2/pokemon/4/"))
            )
            return TipoDto("fire", if (duplicados) referencias + referencias else referencias)
        }
        override suspend fun obtenerDetalle(nombre: String): ItemDto {
            llamadasDetalle++
            bloqueoDetalle?.await()
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
