package com.tecsup.pokemon.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.components.Cargando

@Composable
fun AdminScreen(viewModel: AdminViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val ocupado by viewModel.ocupado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    var confirmarLimpieza by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Panel de administración", style = MaterialTheme.typography.headlineSmall)

        when (val actual = estado) {
            UiState.Cargando -> Cargando(Modifier.fillMaxWidth())
            is UiState.Error -> Text(actual.mensaje, color = MaterialTheme.colorScheme.error)
            is UiState.Exito -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Total("Pokémon guardados", actual.datos.totalGuardados, Modifier.weight(1f))
                Total("Favoritos", actual.datos.totalFavoritos, Modifier.weight(1f))
            }
        }

        Button(onClick = viewModel::sincronizar, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) {
            if (ocupado) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Sincronizar")
        }
        OutlinedButton(
            onClick = { confirmarLimpieza = true },
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limpiar caché")
        }
        mensaje?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }

    if (confirmarLimpieza) {
        AlertDialog(
            onDismissRequest = { confirmarLimpieza = false },
            title = { Text("¿Limpiar caché?") },
            text = { Text("Se borrarán los Pokémon guardados en el celular y sus favoritos.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarLimpieza = false
                    viewModel.limpiarCache()
                }) { Text("Limpiar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarLimpieza = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Total(titulo: String, valor: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$valor", style = MaterialTheme.typography.displaySmall)
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
