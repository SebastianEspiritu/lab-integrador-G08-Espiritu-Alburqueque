package com.tecsup.pokemon.ui.favoritos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.components.Cargando

@Composable
fun FavoritosScreen(
    viewModel: FavoritosViewModel,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Mis favoritos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        when (val actual = estado) {
            UiState.Cargando -> Cargando(Modifier.fillMaxSize())
            is UiState.Error -> Text(
                actual.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            is UiState.Exito -> if (actual.datos.isEmpty()) {
                Text(
                    "Todavía no marcaste favoritos. Toca ♡ en la lista o en el detalle de un Pokémon.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(actual.datos, key = { it.id }) { item ->
                        Card(onClick = { onItemClick(item.id) }, modifier = Modifier.fillMaxWidth()) {
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
                                } else {
                                    Text("🔥")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(item.tipos.joinToString(" · "))
                                }
                                IconButton(
                                    onClick = { viewModel.alternarFavorito(item.id) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Quitar ${item.nombre} de favoritos"
                                    }
                                ) {
                                    Text("♥", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
