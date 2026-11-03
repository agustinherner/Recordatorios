package com.example.recordatorios.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel del detalle. Recibe SOLO el identificador que llegó por la ruta
 * y le pide el registro actualizado al repositorio (no recibe el objeto completo).
 */
class DetalleViewModel(
    private val id: Long,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun onReintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { DetalleUiState.Cargando }
            try {
                val recordatorio = repositorio.obtenerPorId(id)
                _uiState.update {
                    if (recordatorio != null) DetalleUiState.Contenido(recordatorio)
                    else DetalleUiState.Error("No encontramos el recordatorio #$id.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { DetalleUiState.Error("No pudimos cargar el recordatorio. Intentá de nuevo.") }
            }
        }
    }
}
