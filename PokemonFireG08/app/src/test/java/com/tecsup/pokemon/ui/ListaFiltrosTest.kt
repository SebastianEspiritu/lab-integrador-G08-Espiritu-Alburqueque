package com.tecsup.pokemon.ui

import androidx.lifecycle.ViewModelStore
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.lista.ListaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListaFiltrosTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val muestras = listOf(
        Item("charmander", "Charmander", tipos = listOf("fire")),
        Item("charizard", "Charizard", tipos = listOf("fire", "flying")),
        Item("vulpix", "Vulpix", tipos = listOf("fire"))
    )
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun limpiar() { store.clear(); Dispatchers.resetMain() }
    private fun crear(repo: FakeRepository): ListaViewModel = ListaViewModel(repo).also { store.put("lista", it) }
    private fun ids(vm: ListaViewModel) = (vm.estado.value as UiState.Exito).datos.map { it.id }

    @Test fun nombreIgnoraMayusculasYEspaciosEnExtremos() = runTest(dispatcher) {
        val repo = FakeRepository(muestras)
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("  CHAR  ")
        advanceUntilIdle()
        assertEquals(listOf("charmander", "charizard"), ids(vm))
        assertEquals(1, repo.refrescos)
    }

    @Test fun tipoSecundarioSoloSeleccionaDatosQueLoContienen() = runTest(dispatcher) {
        val vm = crear(FakeRepository(muestras))
        advanceUntilIdle()
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        assertEquals(listOf("charizard"), ids(vm))
    }

    @Test fun combinaNombreYTipoSinModificarLosDatosLocales() = runTest(dispatcher) {
        val repo = FakeRepository(muestras)
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("char")
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        assertEquals(listOf("charizard"), ids(vm))
        assertEquals(muestras, repo.items.value)
        assertEquals(1, repo.refrescos)
    }

    @Test fun sinCoincidenciasEsExitoVacioConFiltrosActivos() = runTest(dispatcher) {
        val vm = crear(FakeRepository(muestras))
        advanceUntilIdle()
        vm.buscar("vulpix")
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        assertTrue(ids(vm).isEmpty())
        assertTrue(vm.filtrosActivos.value)
    }

    @Test fun limpiarTextoConservaTipoYRestablecerRecuperaTodaLaLista() = runTest(dispatcher) {
        val repo = FakeRepository(muestras)
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("inexistente")
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        vm.buscar("")
        advanceUntilIdle()
        assertEquals(listOf("charizard"), ids(vm))
        vm.restablecerFiltros()
        advanceUntilIdle()
        assertEquals(muestras.map { it.id }, ids(vm))
        assertEquals("", vm.busqueda.value)
        assertNull(vm.tipoSeleccionado.value)
        assertFalse(vm.filtrosActivos.value)
        assertEquals(1, repo.refrescos)
    }

    @Test fun tiposDisponiblesSalenDeLaListaCompletaNoDelResultadoFiltrado() = runTest(dispatcher) {
        val vm = crear(FakeRepository(muestras))
        advanceUntilIdle()
        vm.buscar("vulpix")
        advanceUntilIdle()
        assertEquals(listOf("fire", "flying"), vm.tiposDisponibles.value)
        vm.seleccionarTipo("water")
        advanceUntilIdle()
        assertNull(vm.tipoSeleccionado.value)
        assertEquals(listOf("vulpix"), ids(vm))
    }

    @Test fun nuevasEmisionesLocalesActualizanResultadosSinPeticionAdicional() = runTest(dispatcher) {
        val repo = FakeRepository(muestras.take(1))
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("char")
        advanceUntilIdle()
        repo.items.value = muestras
        advanceUntilIdle()
        assertEquals(listOf("charmander", "charizard"), ids(vm))
        assertEquals(listOf("fire", "flying"), vm.tiposDisponibles.value)
        assertEquals(1, repo.refrescos)
    }

    @Test fun filtrarOfflineConservaAvisoYNoReintentaLaRed() = runTest(dispatcher) {
        val repo = FakeRepository(muestras).apply { fallo = true }
        val vm = crear(repo)
        advanceUntilIdle()
        val aviso = vm.aviso.value
        vm.buscar("CHAR")
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        assertEquals(listOf("charizard"), ids(vm))
        assertNotNull(aviso)
        assertEquals(aviso, vm.aviso.value)
        assertEquals(1, repo.refrescos)
    }

    @Test fun buscarNoOcultaErrorSinDatosNiImpideReintentar() = runTest(dispatcher) {
        val repo = FakeRepository(emptyList()).apply { fallo = true }
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("char")
        advanceUntilIdle()
        assertTrue(vm.estado.value is UiState.Error)
        repo.fallo = false
        repo.items.value = muestras
        vm.cargar()
        advanceUntilIdle()
        assertEquals(listOf("charmander", "charizard"), ids(vm))
        assertEquals("char", vm.busqueda.value)
    }

    @Test fun cambiosRapidosPublicanLaUltimaCombinacionSinSincronizar() = runTest(dispatcher) {
        val repo = FakeRepository(muestras)
        val vm = crear(repo)
        advanceUntilIdle()
        repeat(50) {
            vm.buscar(if (it % 2 == 0) "vulpix" else "char")
            vm.seleccionarTipo(if (it % 2 == 0) "fire" else "flying")
        }
        advanceUntilIdle()
        assertEquals("char", vm.busqueda.value)
        assertEquals("flying", vm.tipoSeleccionado.value)
        assertEquals(listOf("charizard"), ids(vm))
        assertEquals(1, repo.refrescos)
    }

    @Test fun restablecerTrasCambiosRapidosNoDejaResultadosAnteriores() = runTest(dispatcher) {
        val repo = FakeRepository(muestras)
        val vm = crear(repo)
        advanceUntilIdle()
        repeat(20) { vm.buscar("sin coincidencias"); vm.seleccionarTipo("flying") }
        vm.restablecerFiltros()
        advanceUntilIdle()
        assertEquals(muestras.map { it.id }, ids(vm))
        assertFalse(vm.filtrosActivos.value)
        assertEquals(1, repo.refrescos)
    }

    @Test fun listaLocalVaciaPermaneceExitoAlBuscarYRestablecer() = runTest(dispatcher) {
        val repo = FakeRepository(emptyList())
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("char")
        advanceUntilIdle()
        assertTrue(ids(vm).isEmpty())
        assertTrue(vm.filtrosActivos.value)
        assertTrue(vm.tiposDisponibles.value.isEmpty())
        vm.restablecerFiltros()
        advanceUntilIdle()
        assertTrue(ids(vm).isEmpty())
        assertFalse(vm.filtrosActivos.value)
    }

    @Test fun escribirDuranteCargaConservaCargandoYAplicaUltimaBusqueda() = runTest(dispatcher) {
        val repo = FakeRepository(emptyList()).apply { barrera = CompletableDeferred() }
        val vm = crear(repo)
        runCurrent()
        vm.buscar("vul")
        vm.buscar("CHAR")
        runCurrent()
        assertEquals(UiState.Cargando, vm.estado.value)
        repo.items.value = muestras
        repo.barrera!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("charmander", "charizard"), ids(vm))
        assertEquals(1, repo.refrescos)
    }

    @Test fun reintentarConservaAmbosFiltrosYLimpiaAvisoOffline() = runTest(dispatcher) {
        val repo = FakeRepository(muestras).apply { fallo = true }
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("char")
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        assertNotNull(vm.aviso.value)
        repo.fallo = false
        vm.cargar()
        advanceUntilIdle()
        assertEquals(listOf("charizard"), ids(vm))
        assertEquals("char", vm.busqueda.value)
        assertEquals("flying", vm.tipoSeleccionado.value)
        assertNull(vm.aviso.value)
        assertEquals(2, repo.refrescos)
    }

    @Test fun datosLocalesYFiltrosCambiadosEnElMismoCicloSonConsistentes() = runTest(dispatcher) {
        val repo = FakeRepository(muestras)
        val vm = crear(repo)
        advanceUntilIdle()
        vm.buscar("vulpix")
        vm.seleccionarTipo("fire")
        repo.items.value = muestras.take(2)
        vm.buscar("char")
        vm.seleccionarTipo("flying")
        advanceUntilIdle()
        assertEquals(listOf("charizard"), ids(vm))
        assertEquals(1, repo.refrescos)
    }

    private class FakeRepository(iniciales: List<Item>) : ItemRepository {
        override val items = MutableStateFlow(iniciales)
        override val favoritos = items.map { lista -> lista.filter { it.esFavorito } }
        var refrescos = 0
        var fallo = false
        var barrera: CompletableDeferred<Unit>? = null
        override suspend fun refrescar(): Result<Int> {
            refrescos++
            barrera?.await()
            return if (fallo) Result.failure(IllegalStateException("Sin conexión")) else Result.success(items.value.size)
        }
        override suspend fun obtenerDetalle(id: String): Result<Item> = Result.success(items.value.first { it.id == id })
        override suspend fun alternarFavorito(id: String) {
            items.value = items.value.map { if (it.id == id) it.copy(esFavorito = !it.esFavorito) else it }
        }
        override suspend fun cantidadGuardada() = items.value.size
        override suspend fun cantidadFavoritos() = items.value.count { it.esFavorito }
        override suspend fun limpiarCache() { items.value = emptyList() }
    }
}
