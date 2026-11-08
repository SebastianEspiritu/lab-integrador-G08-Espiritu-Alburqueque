package com.tecsup.pokemon

import android.content.Context
import androidx.room.Room
import com.tecsup.pokemon.data.auth.AuthRepository
import com.tecsup.pokemon.data.auth.FirebaseAuthRepository
import com.tecsup.pokemon.data.local.AppDatabase
import com.tecsup.pokemon.data.remote.RetrofitClient
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.data.repository.ItemRepositoryImpl
import com.tecsup.pokemon.notification.Notificador
import com.tecsup.pokemon.notification.NotificadorAndroid

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "pokemon-fire.db").build()
    }
    val itemRepository: ItemRepository by lazy {
        ItemRepositoryImpl(RetrofitClient.apiService, database.itemDao())
    }
    // Las pantallas y los ViewModel dependen de la interface, no de Firebase.
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val notificador: Notificador by lazy { NotificadorAndroid(appContext) }
}
