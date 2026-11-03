package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.DatosNuevoRecordatorio
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.crearRecordatorio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.io.IOException

/**
 * Implementación en memoria del repositorio. Los datos se pierden al cerrar
 * la app: la persistencia se incorpora en módulos posteriores.
 *
 * Las demoras (delay) simulan una consulta lenta. Como son funciones
 * suspendidas, no bloquean la interfaz: la coroutine se suspende y la
 * pantalla sigue respondiendo.
 */
class RecordatorioRepositoryEnMemoria(
    private val demoraCargaMs: Long = 1_500,
    private val demoraGuardadoMs: Long = 1_500,
    private val demoraDetalleMs: Long = 600
) : RecordatorioRepository, ControlSimulacion {

    // Fuente de verdad de esta etapa: un StateFlow con la lista actual.
    private val recordatorios = MutableStateFlow(DatosSimulados.recordatorios)

    // Banderas de simulación. Son "de un solo uso": después de fallar una vez
    // se apagan, así el botón "Reintentar" demuestra la recuperación.
    @Volatile private var fallarCarga = false
    @Volatile private var fallarGuardado = false

    override fun observarRecordatorios(): Flow<List<Recordatorio>> = flow {
        delay(demoraCargaMs)                       // consulta inicial simulada
        if (fallarCarga) {
            fallarCarga = false
            throw IOException("Error simulado al cargar los recordatorios")
        }
        emitAll(recordatorios)                     // desde acá emite cada cambio de la colección
    }

    override suspend fun obtenerPorId(id: Long): Recordatorio? {
        delay(demoraDetalleMs)
        return recordatorios.value.find { it.id == id }
    }

    override suspend fun guardar(nuevo: DatosNuevoRecordatorio): Recordatorio {
        delay(demoraGuardadoMs)
        if (fallarGuardado) {
            fallarGuardado = false
            throw IOException("Error simulado al guardar")
        }
        val siguienteId = (recordatorios.value.maxOfOrNull { it.id } ?: 0L) + 1
        // Se reutiliza la validación de la Actividad 1: si los datos fueran
        // inválidos, getOrThrow() lanza la excepción y el ViewModel la informa.
        val creado = crearRecordatorio(
            id = siguienteId,
            titulo = nuevo.titulo,
            categoria = nuevo.categoria,
            prioridad = nuevo.prioridad,
            descripcion = nuevo.descripcion
        ).getOrThrow()
        recordatorios.update { lista -> lista + creado }   // nueva lista inmutable
        return creado
    }

    override fun categoriasDisponibles(): List<Categoria> = DatosSimulados.categoriasDisponibles

    // --- Simulación -------------------------------------------------------

    override fun vaciarDatos() = recordatorios.update { emptyList() }
    override fun restaurarDatos() = recordatorios.update { DatosSimulados.recordatorios }
    override fun fallarProximaCarga() { fallarCarga = true }
    override fun fallarProximoGuardado() { fallarGuardado = true }
}
