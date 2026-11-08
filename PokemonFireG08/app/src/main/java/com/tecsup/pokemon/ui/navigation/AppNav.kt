package com.tecsup.pokemon.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.ui.detalle.DetalleScreen
import com.tecsup.pokemon.ui.detalle.DetalleViewModel
import com.tecsup.pokemon.ui.lista.ListaScreen
import com.tecsup.pokemon.ui.lista.ListaViewModel

@Composable
fun AppNav(container: AppContainer, modifier: Modifier = Modifier) {
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
