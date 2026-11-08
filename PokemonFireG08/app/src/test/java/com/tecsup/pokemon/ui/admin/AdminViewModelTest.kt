package com.tecsup.pokemon.ui.admin

import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.fakes.FakeItemRepository
import com.tecsup.pokemon.fakes.FakeNotificador
import com.tecsup.pokemon.fakes.MainDispatcherRule
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val guardados = listOf(
        Item("charmander", "Charmander", esFavorito = true),
        Item("vulpix", "Vulpix")
    )

    @Test fun muestraTotalGuardadoYTotalDeFavoritos() = runTest(main.dispatcher) {
        val vm = main.retener(AdminViewModel(FakeItemRepository(guardados), FakeNotificador()))
        advanceUntilIdle()
        assertEquals(UiState.Exito(ResumenAdmin(totalGuardados = 2, totalFavoritos = 1)), vm.estado.value)
    }

    @Test fun sincronizarActualizaTotalesYNotifica() = runTest(main.dispatcher) {
        val notificador = FakeNotificador()
        val vm = main.retener(AdminViewModel(FakeItemRepository(guardados), notificador))
        vm.sincronizar()
        advanceUntilIdle()
        assertEquals(UiState.Exito(ResumenAdmin(totalGuardados = 3, totalFavoritos = 1)), vm.estado.value)
        assertEquals(listOf("Datos actualizados" to "Se guardaron 3 elementos"), notificador.avisos)
        assertFalse(vm.ocupado.value)
    }

    @Test fun sincronizarSinInternetMuestraMensajeYNoNotifica() = runTest(main.dispatcher) {
        val repo = FakeItemRepository(guardados).apply { fallo = IllegalStateException("Sin conexión") }
        val notificador = FakeNotificador()
        val vm = main.retener(AdminViewModel(repo, notificador))
        vm.sincronizar()
        advanceUntilIdle()
        assertEquals("Sin conexión", vm.mensaje.value)
        assertTrue(notificador.avisos.isEmpty())
    }

    @Test fun limpiarCacheDejaLosTotalesEnCero() = runTest(main.dispatcher) {
        val vm = main.retener(AdminViewModel(FakeItemRepository(guardados), FakeNotificador()))
        vm.limpiarCache()
        advanceUntilIdle()
        assertEquals(UiState.Exito(ResumenAdmin(totalGuardados = 0, totalFavoritos = 0)), vm.estado.value)
    }
}
