package com.example.recordatorios.ui.lista

import com.example.recordatorios.domain.Recordatorio

/**
 * Estado de la pantalla de listado.
 *
 * sealed interface porque las situaciones son EXCLUYENTES: la pantalla no
 * puede estar cargando, vacía y con error al mismo tiempo. Cada estado lleva
 * solo los datos que necesita.
 */
sealed interface ListaUiState {
    data object Cargando : ListaUiState
    data object Vacio : ListaUiState
    data class Contenido(val recordatorios: List<Recordatorio>) : ListaUiState
    data class Error(val mensaje: String) : ListaUiState
}

/** Opciones del menú de simulación, para comprobar todos los estados sin base de datos. */
enum class OpcionSimulacion(val texto: String) {
    LISTA_VACIA("Simular lista vacía"),
    ERROR_CARGA("Simular error al cargar"),
    ERROR_GUARDADO("Simular error en el próximo guardado"),
    RESTAURAR("Restaurar datos de ejemplo")
}
