package com.example.recordatorios.ui.detalle

import com.example.recordatorios.domain.Recordatorio

/** Estado de la pantalla de detalle (alternativas excluyentes). */
sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Contenido(val recordatorio: Recordatorio) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
