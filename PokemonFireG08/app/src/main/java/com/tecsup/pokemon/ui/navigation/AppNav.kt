package com.tecsup.pokemon.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tecsup.pokemon.AppContainer
import com.tecsup.pokemon.domain.model.Rol
import com.tecsup.pokemon.domain.model.Sesion
import com.tecsup.pokemon.ui.UiState
import com.tecsup.pokemon.ui.admin.AdminScreen
import com.tecsup.pokemon.ui.admin.AdminViewModel
import com.tecsup.pokemon.ui.components.Cargando
import com.tecsup.pokemon.ui.components.PedirPermisoNotificaciones
import com.tecsup.pokemon.ui.detalle.DetalleScreen
import com.tecsup.pokemon.ui.detalle.DetalleViewModel
import com.tecsup.pokemon.ui.favoritos.FavoritosScreen
import com.tecsup.pokemon.ui.favoritos.FavoritosViewModel
import com.tecsup.pokemon.ui.lista.ListaScreen
import com.tecsup.pokemon.ui.lista.ListaViewModel
import com.tecsup.pokemon.ui.login.LoginScreen
import com.tecsup.pokemon.ui.login.LoginViewModel

@Composable
fun AppNav(container: AppContainer, modifier: Modifier = Modifier) {
    val sesionViewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory(container))
    val estadoSesion by sesionViewModel.estado.collectAsStateWithLifecycle()
    // Se pide al abrir la app para que la primera sincronización ya pueda avisar.
    PedirPermisoNotificaciones()

    when (val actual = estadoSesion) {
        UiState.Cargando -> Cargando(modifier.fillMaxSize())
        is UiState.Error -> LoginDestino(container, sesionViewModel, modifier)
        is UiState.Exito -> {
            val sesion = actual.datos
            if (sesion == null) LoginDestino(container, sesionViewModel, modifier)
            else Catalogo(container, sesion, onCerrarSesion = sesionViewModel::cerrarSesion, modifier = modifier)
        }
    }
}

@Composable
private fun LoginDestino(container: AppContainer, sesionViewModel: SesionViewModel, modifier: Modifier) {
    val vm: LoginViewModel = viewModel(factory = LoginViewModel.Factory(container))
    LoginScreen(vm, onIngreso = sesionViewModel::sesionIniciada, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Catalogo(
    container: AppContainer,
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier
) {
    val navController = rememberNavController()
    Scaffold(
        modifier = modifier,
        // MainActivity ya aplica los insets del sistema.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Column {
                        Text(sesion.correo, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (sesion.rol == Rol.ADMIN) "Administrador" else "Usuario",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = { TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") } }
            )
        },
        bottomBar = { BarraInferior(navController, destinosPara(sesion)) }
    ) { padding ->
        CatalogoNavHost(container, sesion, navController, Modifier.padding(padding))
    }
}

private data class Destino(val ruta: String, val titulo: String, val icono: String)

private val DESTINO_LISTA = Destino("lista", "Catálogo", "🔥")
private val DESTINO_FAVORITOS = Destino("favoritos", "Favoritos", "♥")
private val DESTINO_ADMIN = Destino("admin", "Admin", "⚙")

// R02: el rol viene de Firestore; aquí solo se decide qué pestañas se muestran.
private fun destinosPara(sesion: Sesion): List<Destino> =
    if (sesion.rol == Rol.ADMIN) listOf(DESTINO_LISTA, DESTINO_FAVORITOS, DESTINO_ADMIN)
    else listOf(DESTINO_LISTA, DESTINO_FAVORITOS)

@Composable
private fun BarraInferior(navController: NavHostController, destinos: List<Destino>) {
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    NavigationBar(windowInsets = WindowInsets(0)) {
        destinos.forEach { destino ->
            NavigationBarItem(
                selected = rutaActual == destino.ruta,
                onClick = {
                    navController.navigate(destino.ruta) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Text(destino.icono) },
                label = { Text(destino.titulo) }
            )
        }
    }
}

@Composable
private fun CatalogoNavHost(
    container: AppContainer,
    sesion: Sesion,
    navController: NavHostController,
    modifier: Modifier
) {
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
        composable("favoritos") {
            val vm: FavoritosViewModel = viewModel(factory = FavoritosViewModel.Factory(container))
            FavoritosScreen(vm, onItemClick = { navController.navigate("detalle/${Uri.encode(it)}") { launchSingleTop = true } })
        }
        // La ruta admin ni siquiera existe para un USUARIO.
        if (sesion.rol == Rol.ADMIN) {
            composable("admin") {
                val vm: AdminViewModel = viewModel(factory = AdminViewModel.Factory(container))
                AdminScreen(vm)
            }
        }
    }
}
