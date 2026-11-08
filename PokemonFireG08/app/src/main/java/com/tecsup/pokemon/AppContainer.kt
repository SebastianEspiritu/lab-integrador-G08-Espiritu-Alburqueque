package com.tecsup.pokemon

import android.content.Context
import androidx.room.Room
import com.tecsup.pokemon.data.local.AppDatabase
import com.tecsup.pokemon.data.remote.RetrofitClient
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.data.repository.ItemRepositoryImpl

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "pokemon-fire.db").build()
    }
    val itemRepository: ItemRepository by lazy {
        ItemRepositoryImpl(RetrofitClient.apiService, database.itemDao())
    }
}
