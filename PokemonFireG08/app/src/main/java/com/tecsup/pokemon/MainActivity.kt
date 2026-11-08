package com.tecsup.pokemon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.tecsup.pokemon.ui.navigation.AppNav
import com.tecsup.pokemon.ui.theme.PokemonFireG08Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokemonFireG08Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNav(
                        container = (application as PokemonApplication).appContainer,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
