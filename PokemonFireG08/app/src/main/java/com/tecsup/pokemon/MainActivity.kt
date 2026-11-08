package com.tecsup.pokemon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.pokemon.ui.lista.ListaScreen
import com.tecsup.pokemon.ui.lista.ListaViewModel
import com.tecsup.pokemon.ui.theme.PokemonFireG08Theme

class MainActivity : ComponentActivity() {
    private val appContainer = AppContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val listaViewModel: ListaViewModel = viewModel(
                factory = ListaViewModel.Factory(appContainer)
            )
            PokemonFireG08Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ListaScreen(
                        viewModel = listaViewModel,
                        // Integración temporal: la navegación se añadirá en su hito.
                        onItemClick = { _ -> },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}