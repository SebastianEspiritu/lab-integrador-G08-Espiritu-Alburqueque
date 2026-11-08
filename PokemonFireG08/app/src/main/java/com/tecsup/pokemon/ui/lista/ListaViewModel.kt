package com.tecsup.pokemon.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ListaViewModel(private val cargarItems: () -> List<Item>) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<List<Item>>>(UiState.Cargando)
    val estado: StateFlow<UiState<List<Item>>> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = UiState.Cargando
        _estado.value = try {
            UiState.Exito(cargarItems())
        } catch (_: Exception) {
            UiState.Error("No se pudo cargar la lista de Pokémon.")
        }
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ListaViewModel::class.java)) {
                "ViewModel no compatible con ListaViewModel.Factory"
            }
            @Suppress("UNCHECKED_CAST")
            return ListaViewModel(appContainer::cargarItemsSimulados) as T
        }
    }
}
