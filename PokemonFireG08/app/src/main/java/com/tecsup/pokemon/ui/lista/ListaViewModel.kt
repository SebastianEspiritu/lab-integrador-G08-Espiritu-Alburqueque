package com.tecsup.pokemon.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.notification.Notificador
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class ListaViewModel(
    private val repo: ItemRepository,
    // Interface: el ViewModel no recibe Context. En pruebas se usa uno falso.
    private val notificador: Notificador = SinNotificaciones
) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<List<Item>>>(UiState.Cargando)
    private val _busqueda = MutableStateFlow("")
    val busqueda = _busqueda.asStateFlow()
    private val _tipoSeleccionado = MutableStateFlow<String?>(null)
    val tipoSeleccionado = _tipoSeleccionado.asStateFlow()

    // Se filtra el estado local, sin volver a llamar a refrescar ni a la API.
    val estado: StateFlow<UiState<List<Item>>> = combine(_estado, _busqueda, _tipoSeleccionado) { actual, texto, tipo ->
        if (actual is UiState.Exito) {
            val consulta = texto.trim()
            UiState.Exito(actual.datos.filter { item ->
                item.nombre.contains(consulta, ignoreCase = true) &&
                    (tipo == null || item.tipos.any { it.equals(tipo, ignoreCase = true) })
            })
        } else actual
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Cargando)

    val tiposDisponibles = _estado.map { actual ->
        if (actual is UiState.Exito) {
            actual.datos.flatMap { it.tipos }.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }.distinct().sorted()
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filtrosActivos = combine(_busqueda, _tipoSeleccionado) { texto, tipo ->
        texto.isNotBlank() || tipo != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun buscar(texto: String) { _busqueda.value = texto }
    fun seleccionarTipo(tipo: String?) {
        val normalizado = tipo?.trim()?.lowercase()
        if (normalizado == null || normalizado in tiposDisponibles.value) {
            _tipoSeleccionado.value = normalizado
        }
    }
    fun restablecerFiltros() {
        _busqueda.value = ""
        _tipoSeleccionado.value = null
    }
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
                onSuccess = { guardados ->
                    _estado.value = UiState.Exito(repo.items.first())
                    cargado = true
                    // R09: aviso local al terminar de sincronizar.
                    notificador.mostrar("Datos actualizados", "Se guardaron $guardados elementos")
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

    fun alternarFavorito(id: String) {
        viewModelScope.launch { repo.alternarFavorito(id) }
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ListaViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ListaViewModel(appContainer.itemRepository, appContainer.notificador) as T
        }
    }
}

/** Notificador vacío para cuando no se necesita avisar (por ejemplo, pruebas antiguas). */
private object SinNotificaciones : Notificador {
    override fun mostrar(titulo: String, texto: String) = Unit
}
