package com.tecsup.pokemon.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Pokémon Fire · G08",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        when (val actual = estado) {
            UiState.Cargando -> Cargando(modifier = Modifier.fillMaxSize())
            is UiState.Error -> ErrorView(
                mensaje = actual.mensaje,
                onReintentar = viewModel::cargar,
                modifier = Modifier.fillMaxSize()
            )
            is UiState.Exito -> {
                if (actual.datos.isEmpty()) {
                    Text("No hay Pokémon para mostrar.", modifier = Modifier.padding(16.dp))
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
                                    // H4 no proporciona imágenes ni realiza descargas.
                                    Text(
                                        text = "🔥",
                                        modifier = Modifier.semantics {
                                            contentDescription = "Imagen no disponible"
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(item.tipos.joinToString(" · "))
                                    }
                                    Text(
                                        text = if (item.esFavorito) "♥" else "♡",
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.semantics {
                                            contentDescription = if (item.esFavorito) {
                                                "Favorito"
                                            } else {
                                                "No es favorito"
                                            }
                                        }
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
