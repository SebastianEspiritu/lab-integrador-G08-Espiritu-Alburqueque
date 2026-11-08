package com.tecsup.pokemon.fakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Usa un dispatcher de prueba como Main y limpia los ViewModel al terminar. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    private val store = ViewModelStore()
    private var contador = 0

    fun <T : ViewModel> retener(vm: T): T = vm.also { store.put("vm${contador++}", it) }

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) {
        store.clear()
        Dispatchers.resetMain()
    }
}
