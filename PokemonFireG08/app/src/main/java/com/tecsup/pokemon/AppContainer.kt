package com.tecsup.pokemon

import com.tecsup.pokemon.data.remote.RetrofitClient
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.data.repository.ItemRepositoryImpl

class AppContainer {
    val itemRepository: ItemRepository by lazy {
        ItemRepositoryImpl(RetrofitClient.apiService)
    }
}
