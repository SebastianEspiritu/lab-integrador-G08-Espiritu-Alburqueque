package com.tecsup.pokemon.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetalleViewModel(private val repo: ItemRepository, private val id: String) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<Item>>(UiState.Cargando)
    val estado = _estado.asStateFlow()
    private var carga: Job? = null

    init { cargar() }

    fun cargar() {
        if (carga?.isActive == true) return
        carga = viewModelScope.launch {
            _estado.value = UiState.Cargando
            _estado.value = repo.obtenerDetalle(id).fold(
                onSuccess = { UiState.Exito(it) },
                onFailure = { UiState.Error(it.message ?: "No se pudo cargar el detalle.") }
            )
        }
    }

    fun alternarFavorito() {
        val actual = _estado.value as? UiState.Exito ?: return
        viewModelScope.launch {
            repo.alternarFavorito(id)
            _estado.value = UiState.Exito(actual.datos.copy(esFavorito = !actual.datos.esFavorito))
        }
    }

    class Factory(private val container: AppContainer, private val id: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DetalleViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return DetalleViewModel(container.itemRepository, id) as T
        }
    }
}
