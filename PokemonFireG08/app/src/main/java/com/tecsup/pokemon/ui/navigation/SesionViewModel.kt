package com.tecsup.pokemon.ui.navigation

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
 * Decide si se muestra el login o el catálogo.
 * Exito(null) = no hay sesión; Exito(sesion) = sesión con su rol leído de Firestore.
 */
class SesionViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<Sesion?>>(UiState.Cargando)
    val estado: StateFlow<UiState<Sesion?>> = _estado.asStateFlow()

    init {
        // R11: al reabrir la app se recupera la sesión guardada por Firebase.
        viewModelScope.launch {
            _estado.value = UiState.Exito(runCatching { auth.sesionActual() }.getOrNull())
        }
    }

    fun sesionIniciada(sesion: Sesion) {
        _estado.value = UiState.Exito(sesion)
    }

    fun cerrarSesion() {
        auth.cerrarSesion()
        _estado.value = UiState.Exito(null)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SesionViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return SesionViewModel(container.authRepository) as T
        }
    }
}
