package com.tecsup.pokemon.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.domain.model.Sesion
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.components.Cargando
import com.tecsup.pokemon.ui.detalle.DetalleScreen
import com.tecsup.pokemon.ui.detalle.DetalleViewModel
import com.tecsup.pokemon.ui.lista.ListaScreen
import com.tecsup.pokemon.ui.lista.ListaViewModel
import com.tecsup.pokemon.ui.login.LoginScreen
import com.tecsup.pokemon.ui.login.LoginViewModel

@Composable
fun AppNav(container: AppContainer, modifier: Modifier = Modifier) {
    val sesionViewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory(container))
    val estadoSesion by sesionViewModel.estado.collectAsStateWithLifecycle()

    when (val actual = estadoSesion) {
        UiState.Cargando -> Cargando(modifier.fillMaxSize())
        is UiState.Error -> LoginDestino(container, sesionViewModel, modifier)
        is UiState.Exito -> {
            val sesion = actual.datos
            if (sesion == null) LoginDestino(container, sesionViewModel, modifier)
            else Catalogo(container, sesion, modifier)
        }
    }
}

@Composable
private fun LoginDestino(container: AppContainer, sesionViewModel: SesionViewModel, modifier: Modifier) {
    val vm: LoginViewModel = viewModel(factory = LoginViewModel.Factory(container))
    LoginScreen(vm, onIngreso = sesionViewModel::sesionIniciada, modifier = modifier)
}

@Composable
private fun Catalogo(container: AppContainer, sesion: Sesion, modifier: Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "lista", modifier = modifier) {
        composable("lista") {
            val vm: ListaViewModel = viewModel(factory = ListaViewModel.Factory(container))
            ListaScreen(vm, onItemClick = { navController.navigate("detalle/${Uri.encode(it)}") { launchSingleTop = true } })
        }
        composable("detalle/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            val vm: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory(container, id))
            DetalleScreen(vm, onVolver = { navController.popBackStack() })
        }
    }
}
