package com.tecsup.pokemon.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.data.auth.AuthRepository
import com.tecsup.pokemon.domain.model.Sesion
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Valida el formulario antes de llamar a Firebase.
 * estado = null significa que todavía no se intentó ingresar.
 */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _correo = MutableStateFlow("")
    val correo: StateFlow<String> = _correo.asStateFlow()

    private val _clave = MutableStateFlow("")
    val clave: StateFlow<String> = _clave.asStateFlow()

    private val _estado = MutableStateFlow<UiState<Sesion>?>(null)
    val estado: StateFlow<UiState<Sesion>?> = _estado.asStateFlow()

    fun cambiarCorreo(valor: String) {
        _correo.value = valor
        limpiarError()
    }

    fun cambiarClave(valor: String) {
        _clave.value = valor
        limpiarError()
    }

    fun ingresar() {
        if (_estado.value == UiState.Cargando) return
        val correo = _correo.value.trim()
        val clave = _clave.value
        val error = validar(correo, clave)
        if (error != null) {
            // PA-04: con campos vacíos no se llama a Firebase.
            _estado.value = UiState.Error(error)
            return
        }
        _estado.value = UiState.Cargando
        viewModelScope.launch {
            _estado.value = auth.iniciarSesion(correo, clave).fold(
                onSuccess = { UiState.Exito(it) },
                onFailure = { UiState.Error(it.message ?: "No se pudo iniciar sesión.") }
            )
        }
    }

    /** La pantalla avisa que ya entregó la sesión; así al volver al login no reingresa sola. */
    fun ingresoConsumido() {
        _estado.value = null
        _clave.value = ""
    }

    private fun limpiarError() {
        if (_estado.value is UiState.Error) _estado.value = null
    }

    private fun validar(correo: String, clave: String): String? = when {
        correo.isEmpty() && clave.isEmpty() -> "Ingresa tu correo y tu contraseña."
        correo.isEmpty() -> "Ingresa tu correo."
        clave.isEmpty() -> "Ingresa tu contraseña."
        !FORMATO_CORREO.matches(correo) -> "El correo no tiene un formato válido."
        else -> null
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LoginViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(container.authRepository) as T
        }
    }

    private companion object {
        // Regex propia: android.util.Patterns no funciona en pruebas unitarias JVM.
        val FORMATO_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
