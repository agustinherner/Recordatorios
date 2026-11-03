package com.example.recordatorios.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.ControlSimulacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.ordenadosParaMostrar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del listado: recibe eventos de la pantalla y produce el ListaUiState.
 *
 * Depende de las INTERFACES RecordatorioRepository y ControlSimulacion (las
 * recibe por constructor), no de una lista concreta. En una prueba se le
 * podría pasar un repositorio falso.
 * No guarda referencias a la Activity ni a componentes visuales.
 */
class ListaViewModel(
    private val repositorio: RecordatorioRepository,
    private val simulacion: ControlSimulacion
) : ViewModel() {

    // Estado modificable PRIVADO; hacia afuera solo se expone la versión de lectura.
    private val _uiState = MutableStateFlow<ListaUiState>(ListaUiState.Cargando)
    val uiState: StateFlow<ListaUiState> = _uiState.asStateFlow()

    private var observacion: Job? = null

    init {
        cargar()
    }

    fun onReintentar() = cargar()

    /** when exhaustivo sobre el enum: cada opción prepara la simulación y vuelve a cargar. */
    fun onSimular(opcion: OpcionSimulacion) {
        when (opcion) {
            OpcionSimulacion.LISTA_VACIA -> simulacion.vaciarDatos()
            OpcionSimulacion.ERROR_CARGA -> simulacion.fallarProximaCarga()
            OpcionSimulacion.ERROR_GUARDADO -> simulacion.fallarProximoGuardado()
            OpcionSimulacion.RESTAURAR -> simulacion.restaurarDatos()
        }
        // El error de guardado se verá al guardar; los demás, al recargar ahora.
        if (opcion != OpcionSimulacion.ERROR_GUARDADO) cargar()
    }

    private fun cargar() {
        observacion?.cancel()   // si había una observación anterior, se cancela antes de empezar otra
        // viewModelScope: la coroutine se cancela sola cuando el ViewModel se destruye.
        observacion = viewModelScope.launch {
            _uiState.update { ListaUiState.Cargando }
            repositorio.observarRecordatorios()
                // catch atrapa los errores del flujo, pero NO la cancelación:
                // así la coroutine puede terminar cuando el alcance deja de existir.
                .catch {
                    _uiState.update {
                        ListaUiState.Error("No pudimos cargar los recordatorios. Revisá tu conexión e intentá de nuevo.")
                    }
                }
                .collect { lista ->
                    // Cada emisión del repositorio (carga inicial, guardado, vaciado)
                    // produce una NUEVA instancia del estado.
                    _uiState.update {
                        if (lista.isEmpty()) ListaUiState.Vacio
                        else ListaUiState.Contenido(lista.ordenadosParaMostrar())
                    }
                }
        }
    }
}
