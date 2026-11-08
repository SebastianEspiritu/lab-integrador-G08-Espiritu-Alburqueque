package com.tecsup.pokemon.ui.favoritos

import com.tecsup.pokemon.domain.model.Item
import com.tecsup.pokemon.fakes.FakeItemRepository
import com.tecsup.pokemon.fakes.MainDispatcherRule
import com.tecsup.pokemon.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritosViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val charmander = Item("charmander", "Charmander", esFavorito = true)
    private val vulpix = Item("vulpix", "Vulpix")

    @Test fun muestraSoloLosFavoritos() = runTest(main.dispatcher) {
        val vm = main.retener(FavoritosViewModel(FakeItemRepository(listOf(charmander, vulpix))))
        advanceUntilIdle()
        assertEquals(UiState.Exito(listOf(charmander)), vm.estado.value)
    }

    @Test fun quitarUnFavoritoLoSacaDeLaPantalla() = runTest(main.dispatcher) {
        val vm = main.retener(FavoritosViewModel(FakeItemRepository(listOf(charmander, vulpix))))
        advanceUntilIdle()
        vm.alternarFavorito("charmander")
        advanceUntilIdle()
        assertEquals(UiState.Exito(emptyList<Item>()), vm.estado.value)
    }

    @Test fun losFavoritosSobrevivenALaSincronizacion() = runTest(main.dispatcher) {
        val repo = FakeItemRepository(listOf(charmander, vulpix))
        val vm = main.retener(FavoritosViewModel(repo))
        repo.refrescar()
        advanceUntilIdle()
        val favoritos = (vm.estado.value as UiState.Exito).datos
        assertEquals(listOf("charmander"), favoritos.map { it.id })
    }
}
