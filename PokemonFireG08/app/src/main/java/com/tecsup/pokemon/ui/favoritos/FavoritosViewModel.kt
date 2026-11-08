package com.tecsup.pokemon.ui.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Los favoritos salen de Room: se conservan al cerrar la app y al sincronizar. */
class FavoritosViewModel(private val repo: ItemRepository) : ViewModel() {
    val estado: StateFlow<UiState<List<Item>>> = repo.favoritos
        .map<List<Item>, UiState<List<Item>>> { UiState.Exito(it) }
        .catch { emit(UiState.Error(it.message ?: "No se pudieron leer los favoritos.")) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Cargando)

    fun alternarFavorito(id: String) {
        viewModelScope.launch { repo.alternarFavorito(id) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FavoritosViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return FavoritosViewModel(container.itemRepository) as T
        }
    }
}
