package com.tecsup.pokemon.fakes

import com.tecsup.pokemon.data.auth.AuthRepository
import com.tecsup.pokemon.data.repository.ItemRepository
import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.domain.model.Sesion
import com.tecsup.pokemon.notification.Notificador
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Repositorio en memoria: imita a Room conservando favoritos al sincronizar. */
class FakeItemRepository(iniciales: List<Item> = emptyList()) : ItemRepository {
    val datos = MutableStateFlow(iniciales)
    var remotos = listOf(
        Item("charmander", "Charmander", tipos = listOf("fire")),
        Item("vulpix", "Vulpix", tipos = listOf("fire")),
        Item("growlithe", "Growlithe", tipos = listOf("fire"))
    )
    var fallo: Exception? = null
    var refrescos = 0

    override val items: Flow<List<Item>> = datos
    override val favoritos: Flow<List<Item>> = datos.map { lista -> lista.filter { it.esFavorito } }

    override suspend fun refrescar(): Result<Int> {
        refrescos++
        fallo?.let { return Result.failure(it) }
        val idsFavoritos = datos.value.filter { it.esFavorito }.map { it.id }.toSet()
        datos.value = remotos.map { it.copy(esFavorito = it.id in idsFavoritos) }
        return Result.success(remotos.size)
    }

    override suspend fun obtenerDetalle(id: String): Result<Item> =
        datos.value.find { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No encontrado"))

    override suspend fun alternarFavorito(id: String) {
        datos.value = datos.value.map { if (it.id == id) it.copy(esFavorito = !it.esFavorito) else it }
    }

    override suspend fun cantidadGuardada(): Int = datos.value.size
    override suspend fun cantidadFavoritos(): Int = datos.value.count { it.esFavorito }
    override suspend fun limpiarCache() { datos.value = emptyList() }
}

/** Reemplaza a Firebase: devuelve la sesión o el error que la prueba indique. */
class FakeAuthRepository(
    var resultado: Result<Sesion> = Result.failure(IllegalStateException("Correo o contraseña incorrectos.")),
    var guardada: Sesion? = null
) : AuthRepository {
    var llamadas = 0
    var cierres = 0
    var barrera: CompletableDeferred<Unit>? = null

    override suspend fun iniciarSesion(correo: String, clave: String): Result<Sesion> {
        llamadas++
        barrera?.await()
        resultado.onSuccess { guardada = it }
        return resultado
    }

    override suspend fun sesionActual(): Sesion? = guardada

    override fun cerrarSesion() {
        cierres++
        guardada = null
    }
}

/** Guarda los avisos en una lista en lugar de mostrarlos en Android. */
class FakeNotificador : Notificador {
    val avisos = mutableListOf<Pair<String, String>>()
    override fun mostrar(titulo: String, texto: String) {
        avisos += titulo to texto
    }
}
