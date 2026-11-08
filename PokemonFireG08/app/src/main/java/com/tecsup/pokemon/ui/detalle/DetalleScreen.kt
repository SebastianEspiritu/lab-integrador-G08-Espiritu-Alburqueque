package com.tecsup.pokemon.ui.detalle

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.tecsup.pokemon.domain.model.EstadisticasBasicas
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.components.Cargando
import com.tecsup.pokemon.ui.components.ErrorView

@Composable
fun DetalleScreen(viewModel: DetalleViewModel, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onVolver) { Text("Volver a la lista") }
        when (val actual = estado) {
            UiState.Cargando -> Cargando(Modifier.fillMaxSize())
            is UiState.Error -> ErrorView(actual.mensaje, viewModel::cargar, Modifier.fillMaxSize())
            is UiState.Exito -> {
                val item = actual.datos
                val desplazable = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                if (horizontal) {
                    // En horizontal: imagen a la izquierda y datos a la derecha.
                    Row(modifier = desplazable, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Imagen(item, tamano = 180.dp)
                            BotonFavorito(item, viewModel::alternarFavorito)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(item.nombre, style = MaterialTheme.typography.headlineMedium)
                            Datos(item)
                        }
                    }
                } else {
                    Column(modifier = desplazable, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(item.nombre, style = MaterialTheme.typography.headlineMedium)
                        BotonFavorito(item, viewModel::alternarFavorito)
                        Imagen(item, tamano = 220.dp)
                        Datos(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun Imagen(item: Item, tamano: Dp) {
    SubcomposeAsyncImage(
        model = item.imagenUrl,
        contentDescription = "Imagen de ${item.nombre}",
        modifier = Modifier.size(tamano),
        loading = { Cargando() },
        error = { Text("Imagen no disponible") }
    )
}

@Composable
private fun BotonFavorito(item: Item, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(if (item.esFavorito) "♥ Quitar de favoritos" else "♡ Agregar a favoritos")
    }
}

@Composable
private fun Datos(item: Item) {
    Text("Tipos: ${item.tipos.joinToString(" · ")}")
    Text("Estadísticas básicas", style = MaterialTheme.typography.titleLarge)
    val stats = item.estadisticas
    if (stats == null) {
        Text("Estadísticas no disponibles")
    } else {
        Estadisticas(stats)
    }
}

@Composable
private fun Estadisticas(stats: EstadisticasBasicas) {
    listOf(
        "HP" to stats.hp,
        "Ataque" to stats.ataque,
        "Defensa" to stats.defensa,
        "Ataque especial" to stats.ataqueEspecial,
        "Defensa especial" to stats.defensaEspecial,
        "Velocidad" to stats.velocidad
    ).forEach { (nombre, valor) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("$nombre: $valor", modifier = Modifier.width(170.dp))
            // Las estadísticas base de Pokémon llegan como máximo a 255.
            LinearProgressIndicator(
                progress = { (valor / 255f).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
