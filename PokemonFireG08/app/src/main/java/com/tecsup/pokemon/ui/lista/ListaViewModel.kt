package com.tecsup.pokemon.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ListaViewModel(private val repo: ItemRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<List<Item>>>(UiState.Cargando)
    val estado: StateFlow<UiState<List<Item>>> = _estado.asStateFlow()
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()
    private var carga: Job? = null
    private var cargado = false

    init {
        viewModelScope.launch {
            repo.items.collect {
                if (it.isNotEmpty() || cargado) _estado.value = UiState.Exito(it)
            }
        }
        cargar()
    }

    fun cargar() {
        if (carga?.isActive == true) return
        carga = viewModelScope.launch {
            cargado = false
            val guardados = repo.items.first()
            _estado.value = if (guardados.isEmpty()) UiState.Cargando else UiState.Exito(guardados)
            _aviso.value = null
            repo.refrescar().fold(
                onSuccess = {
                    _estado.value = UiState.Exito(repo.items.first())
                    cargado = true
                },
                onFailure = {
                    val locales = repo.items.first()
                    if (locales.isEmpty()) {
                        _estado.value = UiState.Error(it.message ?: "No se pudo cargar la lista.")
                    } else {
                        _estado.value = UiState.Exito(locales)
                        _aviso.value = "${it.message ?: "No se pudo sincronizar."} Mostrando datos guardados."
                        cargado = true
                    }
                }
            )
        }
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ListaViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ListaViewModel(appContainer.itemRepository) as T
        }
    }
}
