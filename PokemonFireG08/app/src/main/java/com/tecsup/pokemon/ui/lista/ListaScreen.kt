package com.tecsup.pokemon.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.components.Cargando
import com.tecsup.pokemon.ui.components.ErrorView

@Composable
fun ListaScreen(
    viewModel: ListaViewModel,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val tipo by viewModel.tipoSeleccionado.collectAsStateWithLifecycle()
    val tipos by viewModel.tiposDisponibles.collectAsStateWithLifecycle()
    val filtrosActivos by viewModel.filtrosActivos.collectAsStateWithLifecycle()
    var menuAbierto by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Pokémon Fire · G08",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        OutlinedTextField(
            value = busqueda,
            onValueChange = viewModel::buscar,
            label = { Text("Buscar por nombre") },
            singleLine = true,
            trailingIcon = {
                if (busqueda.isNotEmpty()) TextButton(onClick = { viewModel.buscar("") }) { Text("Limpiar") }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box {
                TextButton(onClick = { menuAbierto = true }) { Text("Tipo: ${tipo ?: "Todos"}") }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Todos los tipos") },
                        onClick = { viewModel.seleccionarTipo(null); menuAbierto = false }
                    )
                    tipos.forEach { disponible ->
                        DropdownMenuItem(
                            text = { Text(disponible) },
                            onClick = { viewModel.seleccionarTipo(disponible); menuAbierto = false }
                        )
                    }
                }
            }
            TextButton(onClick = viewModel::restablecerFiltros, enabled = filtrosActivos) { Text("Restablecer") }
        }
        aviso?.let { mensaje ->
            Text(mensaje, modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(onClick = viewModel::cargar) { Text("Reintentar sincronización") }
        }
        when (val actual = estado) {
            UiState.Cargando -> Cargando(modifier = Modifier.fillMaxSize())
            is UiState.Error -> ErrorView(
                mensaje = actual.mensaje,
                onReintentar = viewModel::cargar,
                modifier = Modifier.fillMaxSize()
            )
            is UiState.Exito -> {
                if (actual.datos.isEmpty()) {
                    Text(
                        if (filtrosActivos) "Ningún Pokémon coincide con la búsqueda y los filtros."
                        else "No hay Pokémon para mostrar.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(actual.datos, key = { it.id }) { item ->
                            Card(
                                onClick = { onItemClick(item.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.imagenUrl != null) {
                                        AsyncImage(
                                            model = item.imagenUrl,
                                            contentDescription = "Imagen de ${item.nombre}",
                                            modifier = Modifier.size(56.dp)
                                        )
                                    } else Text(
                                        text = "🔥",
                                        modifier = Modifier.semantics {
                                            contentDescription = "Imagen no disponible"
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(item.tipos.joinToString(" · "))
                                    }
                                    IconButton(
                                        onClick = { viewModel.alternarFavorito(item.id) },
                                        modifier = Modifier.semantics {
                                            contentDescription = if (item.esFavorito) {
                                                "Quitar ${item.nombre} de favoritos"
                                            } else {
                                                "Agregar ${item.nombre} a favoritos"
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (item.esFavorito) "♥" else "♡",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
