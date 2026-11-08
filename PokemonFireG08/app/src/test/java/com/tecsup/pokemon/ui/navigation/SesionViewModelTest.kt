package com.tecsup.pokemon.ui.navigation

import com.tecsup.pokemon.domain.model.Rol
import com.tecsup.pokemon.domain.model.Sesion
import com.tecsup.pokemon.fakes.FakeAuthRepository
import com.tecsup.pokemon.fakes.MainDispatcherRule
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val usuario = Sesion("uid-usuario", "usuario@g08.com", Rol.USUARIO)

    @Test fun sinSesionGuardadaSeMuestraElLogin() = runTest(main.dispatcher) {
        val vm = main.retener(SesionViewModel(FakeAuthRepository()))
        assertEquals(UiState.Cargando, vm.estado.value)
        advanceUntilIdle()
        assertEquals(UiState.Exito(null), vm.estado.value)
    }

    @Test fun alReabrirSeRecuperaLaSesionGuardada() = runTest(main.dispatcher) {
        val vm = main.retener(SesionViewModel(FakeAuthRepository(guardada = usuario)))
        advanceUntilIdle()
        assertEquals(UiState.Exito(usuario), vm.estado.value)
    }

    @Test fun cerrarSesionVuelveAlLoginYAvisaAFirebase() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository(guardada = usuario)
        val vm = main.retener(SesionViewModel(auth))
        advanceUntilIdle()
        vm.cerrarSesion()
        assertEquals(UiState.Exito(null), vm.estado.value)
        assertEquals(1, auth.cierres)
        assertEquals(null, auth.sesionActual())
    }
}
