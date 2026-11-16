package com.example.recordatorios.ui.detalle

import com.example.recordatorios.domain.Recordatorio

/** Estado de la pantalla de detalle (alternativas excluyentes). */
sealed interface DetalleUiState {
    data object Cargando : DetalleUiState

    /**
     * @param procesando true mientras se aplica un cambio (evita dobles toques).
     * @param mensajeError error de la última acción; el contenido se sigue mostrando.
     */
    data class Contenido(
        val recordatorio: Recordatorio,
        val procesando: Boolean = false,
        val mensajeError: String? = null
    ) : DetalleUiState

    /** El recordatorio ya no existe (por ejemplo, se acaba de eliminar): la pantalla vuelve atrás. */
    data object Eliminado : DetalleUiState

    data class Error(val mensaje: String) : DetalleUiState
}
