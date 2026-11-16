package com.example.recordatorios.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.ControlSimulacionLocal
import com.example.recordatorios.data.EstadoSincronizacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.remoto.ControlServidorSimulado
import com.example.recordatorios.data.remoto.FallaSimulada
import com.example.recordatorios.domain.PreferenciasUsuario
import com.example.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del listado.
 *
 * Combina cinco flujos en un único ListaUiState:
 *  - recordatorios de Room (ya filtrados y ordenados según DataStore),
 *  - preferencias (para el resumen de filtros),
 *  - conectividad,
 *  - cantidad de cambios sin sincronizar,
 *  - estado de la sincronización.
 * Ninguno depende de la red para mostrar datos: si no hay conexión, la lista
 * guardada se sigue viendo y solo se agrega un aviso.
 *
 * Depende de interfaces recibidas por constructor (repositorio, preferencias,
 * flujo de conectividad), no de Room ni de Retrofit.
 */
class ListaViewModel(
    private val repositorio: RecordatorioRepository,
    private val preferencias: PreferenciasRepository,
    conectado: Flow<Boolean>,
    private val simulacionLocal: ControlSimulacionLocal,
    private val controlServidor: ControlServidorSimulado?,
    private val servicio: String
) : ViewModel() {

    /** Mensaje puntual del menú de pruebas (se muestra en un Snackbar y se consume). */
    private val _mensajeEvento = MutableStateFlow<String?>(null)
    val mensajeEvento: StateFlow<String?> = _mensajeEvento

    /** Los recordatorios, o el error de lectura. Un error NO se representa como lista vacía. */
    private val datos: Flow<ResultadoLectura> = repositorio.observarListado()
        .map<List<Recordatorio>, ResultadoLectura> { ResultadoLectura.Datos(it) }
        .catch { emit(ResultadoLectura.Fallo) }

    val uiState: StateFlow<ListaUiState> = combine(
        datos,
        preferencias.preferencias.onStart { emit(PreferenciasUsuario()) },
        conectado.onStart { emit(true) },
        repositorio.observarCambiosSinSincronizar().onStart { emit(0) },
        repositorio.estadoSincronizacion
    ) { lectura, prefs, enLinea, sinSincronizar, sync ->
        ListaUiState(
            cargando = false,
            recordatorios = (lectura as? ResultadoLectura.Datos)?.lista.orEmpty(),
            errorLectura = if (lectura is ResultadoLectura.Fallo)
                "No se pudieron leer los recordatorios guardados. Intentá cerrar y volver a abrir la app." else null,
            preferencias = prefs,
            enLinea = enLinea,
            cambiosSinSincronizar = sinSincronizar,
            sincronizando = sync is EstadoSincronizacion.EnCurso,
            mensajeSincronizacion = (sync as? EstadoSincronizacion.Fallida)?.mensaje,
            mensajeSincronizacionEsError = sync is EstadoSincronizacion.Fallida && !sync.seReintentara,
            servicio = servicio
        )
    }.stateIn(
        scope = viewModelScope,
        // Sigue activo 5 s después de que la pantalla deja de observar (por ejemplo, al rotar).
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListaUiState()
    )

    /** Botón "Sincronizar ahora": reencola los rechazados y pide la sincronización. */
    fun onSincronizar() {
        viewModelScope.launch { repositorio.sincronizarManualmente() }
    }

    fun onSimular(opcion: OpcionSimulacion) {
        val mensaje = when (opcion) {
            OpcionSimulacion.SERVIDOR_ERROR_500 -> fallaServidor(FallaSimulada.ERROR_SERVIDOR)
            OpcionSimulacion.SERVIDOR_ERROR_400 -> fallaServidor(FallaSimulada.SOLICITUD_INVALIDA)
            OpcionSimulacion.SERVIDOR_TIMEOUT -> fallaServidor(FallaSimulada.TIMEOUT)
            OpcionSimulacion.CAMBIO_OTRO_DISPOSITIVO -> {
                val hecho = controlServidor?.simularCambioRemoto() ?: false
                if (hecho) {
                    repositorio.solicitarSincronizacion()
                    "Se modificó un recordatorio en el servidor. Se descargará en la próxima sincronización."
                } else "No hay servidor simulado o no tiene datos."
            }
            OpcionSimulacion.ERROR_GUARDADO_LOCAL -> {
                simulacionLocal.fallarProximoGuardadoLocal()
                "El próximo guardado local fallará por falta de espacio (simulado)."
            }
            OpcionSimulacion.PREFERENCIA_INVALIDA -> {
                viewModelScope.launch { preferencias.escribirValorInvalidoDePrueba() }
                "Se guardaron valores inválidos en tema y orden: la app debe usar los predeterminados."
            }
        }
        _mensajeEvento.value = mensaje
    }

    private fun fallaServidor(falla: FallaSimulada): String {
        val control = controlServidor ?: return "Solo disponible con el servidor simulado (API_SIMULADA=true)."
        control.fallarProximaSolicitud(falla)
        return "La próxima solicitud al servidor fallará. Tocá \"Sincronizar ahora\" para probarlo."
    }

    fun onMensajeMostrado() {
        _mensajeEvento.value = null
    }

    /** Resultado interno de leer Room: datos o fallo. */
    private sealed interface ResultadoLectura {
        data class Datos(val lista: List<Recordatorio>) : ResultadoLectura
        data object Fallo : ResultadoLectura
    }
}
