package com.tecsup.pokemon.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.components.Cargando
import com.tecsup.pokemon.ui.components.ErrorView

@Composable
fun DetalleScreen(viewModel: DetalleViewModel, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onVolver) { Text("Volver a la lista") }
        when (val actual = estado) {
            UiState.Cargando -> Cargando(Modifier.fillMaxSize())
            is UiState.Error -> ErrorView(actual.mensaje, viewModel::cargar, Modifier.fillMaxSize())
            is UiState.Exito -> {
                val item = actual.datos
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(item.nombre, style = MaterialTheme.typography.headlineMedium)
                    OutlinedButton(onClick = viewModel::alternarFavorito) {
                        Text(if (item.esFavorito) "♥ Quitar de favoritos" else "♡ Agregar a favoritos")
                    }
                    SubcomposeAsyncImage(
                        model = item.imagenUrl,
                        contentDescription = "Imagen de ${item.nombre}",
                        modifier = Modifier.size(220.dp),
                        loading = { Cargando() },
                        error = { Text("Imagen no disponible") }
                    )
                    Text("Tipos: ${item.tipos.joinToString(" · ")}")
                    Text("Estadísticas básicas", style = MaterialTheme.typography.titleLarge)
                    val stats = item.estadisticas
                    if (stats == null) {
                        Text("Estadísticas no disponibles")
                    } else {
                        Text("HP: ${stats.hp}")
                        Text("Ataque: ${stats.ataque}")
                        Text("Defensa: ${stats.defensa}")
                        Text("Ataque especial: ${stats.ataqueEspecial}")
                        Text("Defensa especial: ${stats.defensaEspecial}")
                        Text("Velocidad: ${stats.velocidad}")
                    }
                }
            }
        }
    }
}
