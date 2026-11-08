package com.tecsup.pokemon

import android.app.Application

class PokemonApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
