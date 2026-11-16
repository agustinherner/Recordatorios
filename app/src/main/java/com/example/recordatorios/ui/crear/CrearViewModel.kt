package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.mensajeOperacionLocal
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.DatosRecordatorio
import com.example.recordatorios.domain.Prioridad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel del formulario de creación y EDICIÓN.
 *
 * - [idEditar] = null → crea un recordatorio nuevo.
 * - [idEditar] con valor → precarga ese recordatorio desde Room y lo actualiza.
 *
 * Guardar NO depende de la red: el repositorio lo escribe en Room (queda
 * "Pendiente de sincronizar") y la sincronización se hace después. Si la
 * escritura local falla, el formulario CONSERVA lo ingresado y ofrece reintentar.
 */
class CrearViewModel(
    private val idEditar: String?,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CrearUiState(
            modoEdicion = idEditar != null,
            cargandoDatos = idEditar != null,
            categoriasDisponibles = repositorio.categoriasDisponibles()
        )
    )
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    init {
        if (idEditar != null) precargar(idEditar)
    }

    private fun precargar(id: String) {
        viewModelScope.launch {
            try {
                val actual = repositorio.obtenerPorId(id)
                _uiState.update { estado ->
                    if (actual == null) {
                        estado.copy(cargandoDatos = false, errorCarga = "El recordatorio ya no existe.")
                    } else {
                        estado.copy(
                            cargandoDatos = false,
                            titulo = actual.titulo,
                            descripcion = actual.descripcion.orEmpty(),
                            fechaLimite = actual.fechaLimite.orEmpty(),
                            categoria = actual.categoria,
                            prioridad = actual.prioridad,
                            // Una categoría que vino del servidor y no está en la lista se agrega para poder conservarla.
                            categoriasDisponibles = (estado.categoriasDisponibles + actual.categoria).distinct()
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(cargandoDatos = false, errorCarga = "No se pudo leer el recordatorio.") }
            }
        }
    }

    fun onTituloChange(nuevo: String) =
        _uiState.update { it.copy(titulo = nuevo, tituloTocado = true) }

    fun onDescripcionChange(nueva: String) =
        _uiState.update { it.copy(descripcion = nueva) }

    fun onFechaChange(nueva: String) =
        _uiState.update { it.copy(fechaLimite = nueva) }

    fun onCategoriaSeleccionada(categoria: Categoria) = _uiState.update { estado ->
        if (categoria in estado.categoriasDisponibles) estado.copy(categoria = categoria) else estado
    }

    fun onPrioridadSeleccionada(prioridad: Prioridad) =
        _uiState.update { it.copy(prioridad = prioridad) }

    fun onGuardar() {
        val estado = _uiState.value
        // Defensa extra: aunque el botón esté deshabilitado, se vuelve a validar acá.
        if (!estado.puedeGuardar) {
            _uiState.update { it.copy(tituloTocado = true) }
            return
        }
        val categoria = estado.categoria ?: return
        val prioridad = estado.prioridad ?: return

        // Enviando ANTES de lanzar la coroutine: un segundo toque encuentra
        // puedeGuardar = false y no genera un registro duplicado.
        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }

        val datos = DatosRecordatorio(
            titulo = estado.titulo.trim(),
            descripcion = estado.descripcion.trim().ifEmpty { null },
            categoria = categoria,
            prioridad = prioridad,
            fechaLimite = estado.fechaLimite.trim().ifEmpty { null }
        )

        viewModelScope.launch {
            val resultado = if (idEditar == null) {
                repositorio.crear(datos).map { }
            } else {
                repositorio.actualizar(idEditar, datos)
            }
            _uiState.update { actual ->
                resultado.fold(
                    onSuccess = { actual.copy(envio = EstadoEnvio.Exito) },
                    // Falla: se conserva todo lo ingresado y se muestra un mensaje claro.
                    onFailure = { error -> actual.copy(envio = EstadoEnvio.Error(error.mensajeOperacionLocal())) }
                )
            }
        }
    }

    /**
     * La navegación de regreso es un EVENTO de una sola vez: después de que la
     * pantalla lo procesa, el estado vuelve a Editando.
     */
    fun onExitoProcesado() = _uiState.update { it.copy(envio = EstadoEnvio.Editando) }
}
