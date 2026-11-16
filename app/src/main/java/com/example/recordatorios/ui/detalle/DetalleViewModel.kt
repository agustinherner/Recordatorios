package com.example.recordatorios.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.mensajeOperacionLocal
import com.example.recordatorios.domain.EstadoRecordatorio
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel del detalle. Recibe SOLO el identificador que llegó por la ruta.
 *
 * Actividad 3: ya no hace una lectura puntual, OBSERVA el registro en Room
 * (Flow). Si el recordatorio cambia —porque se editó, se completó o porque
 * llegó una versión nueva desde el servidor— la pantalla se actualiza sola.
 */
class DetalleViewModel(
    private val id: String,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    private var observacion: Job? = null

    init {
        observar()
    }

    fun onReintentar() = observar()

    private fun observar() {
        observacion?.cancel()
        observacion = viewModelScope.launch {
            var huboContenido = false
            repositorio.observarRecordatorio(id)
                .catch { _uiState.value = DetalleUiState.Error("No pudimos leer el recordatorio. Intentá de nuevo.") }
                .collect { recordatorio ->
                    _uiState.update { anterior ->
                        when {
                            recordatorio != null -> {
                                huboContenido = true
                                // Se conserva el error de la última acción si lo había.
                                DetalleUiState.Contenido(
                                    recordatorio = recordatorio,
                                    mensajeError = (anterior as? DetalleUiState.Contenido)?.mensajeError
                                )
                            }
                            // Existía y dejó de existir: se eliminó (acá o en otro dispositivo).
                            huboContenido -> DetalleUiState.Eliminado
                            else -> DetalleUiState.Error("No encontramos el recordatorio.")
                        }
                    }
                }
        }
    }

    /** Pendiente → En curso → Completado → (reabrir) Pendiente. */
    fun onAvanzarEstado() {
        val actual = (_uiState.value as? DetalleUiState.Contenido)?.recordatorio ?: return
        val nuevo = when (actual.estado) {
            EstadoRecordatorio.Pendiente -> EstadoRecordatorio.EnCurso
            EstadoRecordatorio.EnCurso -> EstadoRecordatorio.Completado(hoy())
            is EstadoRecordatorio.Completado -> EstadoRecordatorio.Pendiente
        }
        ejecutar { repositorio.cambiarEstado(id, nuevo) }
    }

    fun onEliminar() = ejecutar { repositorio.eliminar(id) }

    /** Fecha de hoy en formato AAAA-MM-DD. (java.time necesita API 26 y la app soporta desde la 24.) */
    private fun hoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun onErrorMostrado() = _uiState.update {
        if (it is DetalleUiState.Contenido) it.copy(mensajeError = null) else it
    }

    /**
     * Ejecuta una escritura local. El resultado visible llega solo por el Flow
     * de Room; acá únicamente se maneja "procesando" y el posible error.
     */
    private fun ejecutar(operacion: suspend () -> Result<Unit>) {
        val estado = _uiState.value as? DetalleUiState.Contenido ?: return
        if (estado.procesando) return
        _uiState.value = estado.copy(procesando = true, mensajeError = null)
        viewModelScope.launch {
            val resultado = operacion()
            _uiState.update { actual ->
                if (actual !is DetalleUiState.Contenido) actual
                else actual.copy(
                    procesando = false,
                    mensajeError = resultado.exceptionOrNull()?.mensajeOperacionLocal()
                )
            }
        }
    }
}
