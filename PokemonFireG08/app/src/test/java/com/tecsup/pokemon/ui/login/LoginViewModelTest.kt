package com.tecsup.pokemon.ui.login

import com.tecsup.pokemon.domain.model.Rol
import com.tecsup.pokemon.domain.model.Sesion
import com.tecsup.pokemon.fakes.FakeAuthRepository
import com.tecsup.pokemon.fakes.MainDispatcherRule
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val admin = Sesion("uid-admin", "admin@g08.com", Rol.ADMIN)

    @Test fun camposVaciosMuestranValidacionSinLlamarAFirebase() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository()
        val vm = main.retener(LoginViewModel(auth))
        vm.ingresar()
        advanceUntilIdle()
        assertEquals(UiState.Error("Ingresa tu correo y tu contraseña."), vm.estado.value)
        vm.cambiarCorreo("admin@g08.com")
        vm.ingresar()
        advanceUntilIdle()
        assertEquals(UiState.Error("Ingresa tu contraseña."), vm.estado.value)
        assertEquals(0, auth.llamadas)
    }

    @Test fun correoConFormatoInvalidoNoLlamaAFirebase() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository()
        val vm = main.retener(LoginViewModel(auth))
        vm.cambiarCorreo("admin")
        vm.cambiarClave("123456")
        vm.ingresar()
        advanceUntilIdle()
        assertEquals(UiState.Error("El correo no tiene un formato válido."), vm.estado.value)
        assertEquals(0, auth.llamadas)
    }

    @Test fun credencialesIncorrectasMuestranElErrorDelRepositorio() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository()
        val vm = main.retener(LoginViewModel(auth))
        vm.cambiarCorreo("usuario@g08.com")
        vm.cambiarClave("mala")
        vm.ingresar()
        advanceUntilIdle()
        assertEquals(UiState.Error("Correo o contraseña incorrectos."), vm.estado.value)
        assertEquals(1, auth.llamadas)
    }

    @Test fun ingresoCorrectoEntregaLaSesionConSuRol() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository(resultado = Result.success(admin))
        val vm = main.retener(LoginViewModel(auth))
        vm.cambiarCorreo("  admin@g08.com ")
        vm.cambiarClave("123456")
        vm.ingresar()
        advanceUntilIdle()
        assertEquals(UiState.Exito(admin), vm.estado.value)
        vm.ingresoConsumido()
        assertNull(vm.estado.value)
        assertEquals("", vm.clave.value)
    }

    @Test fun mientrasCargaNoSeDuplicaLaLlamada() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository(resultado = Result.success(admin)).apply { barrera = CompletableDeferred() }
        val vm = main.retener(LoginViewModel(auth))
        vm.cambiarCorreo("admin@g08.com")
        vm.cambiarClave("123456")
        vm.ingresar()
        runCurrent()
        vm.ingresar()
        runCurrent()
        assertEquals(UiState.Cargando, vm.estado.value)
        auth.barrera!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, auth.llamadas)
    }
}
