package com.tecsup.pokemon.ui.lista

import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.fakes.FakeItemRepository
import com.tecsup.pokemon.fakes.FakeNotificador
import com.tecsup.pokemon.fakes.MainDispatcherRule
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** R09 y R12: la lista avisa con el Notificador falso, sin Context ni Android. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListaNotificacionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun primeraCargaNotificaCuantosSeGuardaron() = runTest(main.dispatcher) {
        val notificador = FakeNotificador()
        val vm = main.retener(ListaViewModel(FakeItemRepository(), notificador))
        advanceUntilIdle()
        assertEquals(3, (vm.estado.value as UiState.Exito).datos.size)
        assertEquals(listOf("Datos actualizados" to "Se guardaron 3 elementos"), notificador.avisos)
    }

    @Test fun sinConexionNoNotificaYMuestraLoGuardado() = runTest(main.dispatcher) {
        val repo = FakeItemRepository(listOf(Item("charmander", "Charmander"))).apply {
            fallo = IllegalStateException("Sin conexión.")
        }
        val notificador = FakeNotificador()
        val vm = main.retener(ListaViewModel(repo, notificador))
        advanceUntilIdle()
        assertEquals(1, (vm.estado.value as UiState.Exito).datos.size)
        assertTrue(notificador.avisos.isEmpty())
    }

    @Test fun marcarFavoritoDesdeLaListaSeVeEnElEstado() = runTest(main.dispatcher) {
        val vm = main.retener(ListaViewModel(FakeItemRepository(), FakeNotificador()))
        advanceUntilIdle()
        vm.alternarFavorito("vulpix")
        advanceUntilIdle()
        val vulpix = (vm.estado.value as UiState.Exito).datos.first { it.id == "vulpix" }
        assertTrue(vulpix.esFavorito)
    }
}
