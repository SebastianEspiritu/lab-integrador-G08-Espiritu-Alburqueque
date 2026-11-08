package com.tecsup.pokemon.data.remote

import com.tecsup.pokemon.data.remote.dto.ItemDto
import com.tecsup.pokemon.data.remote.dto.TipoDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("type/fire")
    suspend fun listar(): TipoDto

    @GET("pokemon/{nombre}")
    suspend fun obtenerDetalle(@Path("nombre") nombre: String): ItemDto
}
