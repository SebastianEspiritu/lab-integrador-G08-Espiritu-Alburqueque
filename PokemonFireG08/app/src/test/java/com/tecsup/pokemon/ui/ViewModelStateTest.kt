package com.tecsup.pokemon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.detalle.DetalleViewModel
import com.tecsup.pokemon.ui.lista.ListaViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Pruebas de presentación contra el contrato, sin Retrofit, Room ni Android Context. */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun limpiar() { store.clear(); Dispatchers.resetMain() }
    private fun <T : ViewModel> retener(vm: T): T = vm.also { store.put("vm", it) }

    @Test fun listaPermaneceCargandoHastaTerminarSinDuplicarSolicitudes() = runTest(dispatcher) {
        val repo = FakeRepository().apply { barreraLista = CompletableDeferred() }
        val vm = retener(ListaViewModel(repo))
        runCurrent()
        vm.cargar()
        runCurrent()
        assertEquals(UiState.Cargando, vm.estado.value)
        assertEquals(1, repo.refrescos)
        repo.barreraLista!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(UiState.Exito(listOf(repo.item)), vm.estado.value)
    }

    @Test fun listaSinDatosMuestraErrorYSeRecuperaConReintento() = runTest(dispatcher) {
        val repo = FakeRepository().apply { fallo = IllegalStateException("Sin conexión") }
        val vm = retener(ListaViewModel(repo))
        advanceUntilIdle()
        assertEquals(UiState.Error("Sin conexión"), vm.estado.value)
        assertNull(vm.aviso.value)
        repo.fallo = null
        vm.cargar()
        advanceUntilIdle()
        assertTrue(vm.estado.value is UiState.Exito)
        assertEquals(2, repo.refrescos)
    }

    @Test fun listaMuestraLocalesInclusoMientrasEsperaLaRed() = runTest(dispatcher) {
        val repo = FakeRepository().apply {
            items.value = listOf(item)
            barreraLista = CompletableDeferred()
            fallo = IllegalStateException("HTTP 503")
        }
        val vm = retener(ListaViewModel(repo))
        runCurrent()
        assertEquals(UiState.Exito(listOf(repo.item)), vm.estado.value)
        repo.barreraLista!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(UiState.Exito(listOf(repo.item)), vm.estado.value)
        assertTrue(requireNotNull(vm.aviso.value).contains("HTTP 503"))
    }

    @Test fun listaObservaLimpiezaDeCacheSinOtraPeticion() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = retener(ListaViewModel(repo))
        advanceUntilIdle()
        repo.limpiarCache()
        advanceUntilIdle()
        assertEquals(UiState.Exito(emptyList<Item>()), vm.estado.value)
        assertEquals(1, repo.refrescos)
    }

    @Test fun detallePermaneceCargandoHastaRecibirRespuestaSinDuplicar() = runTest(dispatcher) {
        val repo = FakeRepository().apply { barreraDetalle = CompletableDeferred() }
        val vm = retener(DetalleViewModel(repo, repo.item.id))
        runCurrent()
        vm.cargar()
        runCurrent()
        assertEquals(UiState.Cargando, vm.estado.value)
        assertEquals(1, repo.detalles)
        repo.barreraDetalle!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(UiState.Exito(repo.item), vm.estado.value)
    }

    @Test fun detalleMuestraErrorYPermiteReintentarElMismoIdString() = runTest(dispatcher) {
        val repo = FakeRepository().apply { fallo = IllegalStateException("HTTP 404") }
        val vm = retener(DetalleViewModel(repo, "charmander"))
        advanceUntilIdle()
        assertEquals(UiState.Error("HTTP 404"), vm.estado.value)
        repo.fallo = null
        vm.cargar()
        advanceUntilIdle()
        assertEquals(UiState.Exito(repo.item), vm.estado.value)
        assertEquals(listOf("charmander", "charmander"), repo.ids)
    }

    private class FakeRepository : ItemRepository {
        val item = Item("charmander", "Charmander", tipos = listOf("fire"))
        override val items = MutableStateFlow<List<Item>>(emptyList())
        override val favoritos = items.map { lista -> lista.filter { it.esFavorito } }
        var fallo: Exception? = null
        var barreraLista: CompletableDeferred<Unit>? = null
        var barreraDetalle: CompletableDeferred<Unit>? = null
        var refrescos = 0
        var detalles = 0
        val ids = mutableListOf<String>()
        override suspend fun refrescar(): Result<Int> {
            refrescos++
            barreraLista?.await()
            fallo?.let { return Result.failure(it) }
            items.value = listOf(item)
            return Result.success(1)
        }
        override suspend fun obtenerDetalle(id: String): Result<Item> {
            detalles++
            ids += id
            barreraDetalle?.await()
            return fallo?.let { Result.failure(it) } ?: Result.success(item)
        }
        override suspend fun alternarFavorito(id: String) {
            items.value = items.value.map { if (it.id == id) it.copy(esFavorito = !it.esFavorito) else it }
        }
        override suspend fun cantidadGuardada() = items.value.size
        override suspend fun cantidadFavoritos() = items.value.count { it.esFavorito }
        override suspend fun limpiarCache() { items.value = emptyList() }
    }
}
