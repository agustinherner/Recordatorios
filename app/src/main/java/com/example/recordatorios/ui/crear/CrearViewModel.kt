package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.DatosNuevoRecordatorio
import com.example.recordatorios.domain.Prioridad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel del formulario de creación.
 *
 * Recibe los eventos de la pantalla (onTituloChange, onGuardar, …), aplica
 * las reglas y produce un nuevo CrearUiState con copy(). La pantalla nunca
 * modifica el estado directamente. Al vivir en el ViewModel, el formulario
 * sobrevive a la rotación.
 */
class CrearViewModel(
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CrearUiState(categoriasDisponibles = repositorio.categoriasDisponibles())
    )
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    fun onTituloChange(nuevo: String) =
        _uiState.update { it.copy(titulo = nuevo, tituloTocado = true) }

    fun onDescripcionChange(nueva: String) =
        _uiState.update { it.copy(descripcion = nueva) }

    fun onCategoriaSeleccionada(categoria: Categoria) = _uiState.update { estado ->
        // Solo se acepta si pertenece a las opciones definidas.
        if (categoria in estado.categoriasDisponibles) estado.copy(categoria = categoria) else estado
    }

    fun onPrioridadSeleccionada(prioridad: Prioridad) =
        _uiState.update { it.copy(prioridad = prioridad) }

    fun onGuardar() {
        val estado = _uiState.value
        // Defensa extra: aunque el botón esté deshabilitado, se vuelve a validar acá.
        if (!estado.puedeGuardar) {
            _uiState.update { it.copy(tituloTocado = true) }   // muestra los errores pendientes
            return
        }
        val categoria = estado.categoria ?: return
        val prioridad = estado.prioridad ?: return

        // Se pasa a Enviando ANTES de lanzar la coroutine: un segundo toque ya encuentra
        // puedeGuardar = false y no genera un registro duplicado.
        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }

        viewModelScope.launch {
            try {
                val creado = repositorio.guardar(
                    DatosNuevoRecordatorio(
                        titulo = estado.titulo.trim(),
                        descripcion = estado.descripcion.trim().ifEmpty { null },
                        categoria = categoria,
                        prioridad = prioridad
                    )
                )
                _uiState.update { it.copy(envio = EstadoEnvio.Exito(creado.id)) }
            } catch (e: CancellationException) {
                throw e   // la cancelación no se oculta: deja que la coroutine termine
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(envio = EstadoEnvio.Error("No se pudo guardar el recordatorio. Intentá nuevamente."))
                }
            }
        }
    }

    /**
     * La navegación de regreso es un EVENTO de una sola vez: después de que la
     * pantalla lo procesa, el estado vuelve a Editando para que una
     * recomposición no repita la navegación.
     */
    fun onExitoProcesado() = _uiState.update { it.copy(envio = EstadoEnvio.Editando) }
}
