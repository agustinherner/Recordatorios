package com.example.recordatorios.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.recordatorios.ui.crear.CrearDestino
import com.example.recordatorios.ui.detalle.DetalleDestino
import com.example.recordatorios.ui.lista.ListaDestino

/**
 * Grafo de navegación de la app.
 *
 * El NavController se crea y se usa SOLO acá. Las pantallas no lo reciben:
 * comunican la intención de la persona con lambdas (onCrearRecordatorio,
 * onRecordatorioClick, onGuardado, onCancelar, onVolver) y este grafo decide
 * a qué ruta corresponde cada una.
 */
@Composable
fun NavegacionRecordatorios(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ListaRoute,
        modifier = modifier
    ) {
        composable<ListaRoute> {
            ListaDestino(
                // launchSingleTop evita apilar dos veces el mismo destino si se toca dos veces rápido.
                onCrearRecordatorio = { navController.navigate(CrearRoute) { launchSingleTop = true } },
                onRecordatorioClick = { id -> navController.navigate(DetalleRoute(id)) { launchSingleTop = true } }
            )
        }
        composable<CrearRoute> {
            CrearDestino(
                // Después de guardar o cancelar se VUELVE con popBackStack():
                // no se navega otra vez a ListaRoute, que agregaría una copia del listado.
                onGuardado = { navController.volver() },
                onCancelar = { navController.volver() }
            )
        }
        composable<DetalleRoute> { entrada ->
            // toRoute reconstruye el objeto DetalleRoute con su tipo original (id: Long).
            val ruta = entrada.toRoute<DetalleRoute>()
            DetalleDestino(id = ruta.id, onVolver = { navController.volver() })
        }
    }
}

/**
 * popBackStack() protegido: solo actúa si el destino actual está activo
 * (RESUMED). Evita que un doble toque en "Volver" saque dos entradas y
 * deje la pantalla en blanco.
 */
private fun NavController.volver() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}
