package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.DatosNuevoRecordatorio
import com.example.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de recordatorios: el ÚNICO punto de entrada a los datos.
 *
 * Declara QUÉ operaciones existen, no CÓMO se resuelven. Los ViewModel
 * dependen de esta interfaz, así que la implementación en memoria de esta
 * etapa podrá reemplazarse por Room o un servicio remoto sin tocar las
 * pantallas ni los componentes.
 */
interface RecordatorioRepository {

    /** Flujo con la colección actual. Emite de nuevo cada vez que cambia (por ejemplo, al guardar). */
    fun observarRecordatorios(): Flow<List<Recordatorio>>

    /** Busca un recordatorio por su identificador; null si no existe. */
    suspend fun obtenerPorId(id: Long): Recordatorio?

    /** Guarda un recordatorio nuevo y lo devuelve con el id asignado. Puede fallar. */
    suspend fun guardar(nuevo: DatosNuevoRecordatorio): Recordatorio

    /** Categorías que se pueden elegir en el formulario. */
    fun categoriasDisponibles(): List<Categoria>
}

/**
 * Mecanismo para SIMULAR situaciones sin base de datos ni servicio remoto
 * (consigna 3): lista vacía, error de carga y error de guardado.
 * Está separado del repositorio para que la interfaz de datos quede limpia.
 */
interface ControlSimulacion {
    fun vaciarDatos()
    fun restaurarDatos()
    fun fallarProximaCarga()
    fun fallarProximoGuardado()
}
