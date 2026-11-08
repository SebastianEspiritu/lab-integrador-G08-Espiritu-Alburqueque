package com.tecsup.pokemon.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResumenAdmin(val totalGuardados: Int, val totalFavoritos: Int)

/** Panel solo para ADMIN: totales de Room, sincronizar y limpiar caché. */
class AdminViewModel(private val repo: ItemRepository) : ViewModel() {
    // Los totales se recalculan solos cuando Room cambia (favoritos, sincronizar, limpiar).
    val estado: StateFlow<UiState<ResumenAdmin>> =
        combine(repo.items, repo.favoritos) { items, favoritos ->
            UiState.Exito(ResumenAdmin(items.size, favoritos.size)) as UiState<ResumenAdmin>
        }
            .catch { emit(UiState.Error(it.message ?: "No se pudieron leer los totales.")) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Cargando)

    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun sincronizar() {
        if (_ocupado.value) return
        _ocupado.value = true
        viewModelScope.launch {
            repo.refrescar()
                .onSuccess { n -> _mensaje.value = "Sincronización completa: se guardaron $n Pokémon." }
                .onFailure { _mensaje.value = it.message ?: "No se pudo sincronizar." }
            _ocupado.value = false
        }
    }

    fun limpiarCache() {
        if (_ocupado.value) return
        _ocupado.value = true
        viewModelScope.launch {
            _mensaje.value = runCatching { repo.limpiarCache() }.fold(
                onSuccess = { "Se limpió la caché. Pulsa Sincronizar para volver a descargar." },
                onFailure = { it.message ?: "No se pudo limpiar la caché." }
            )
            _ocupado.value = false
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AdminViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(container.itemRepository) as T
        }
    }
}
