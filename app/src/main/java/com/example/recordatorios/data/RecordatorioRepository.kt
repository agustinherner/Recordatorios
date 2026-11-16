package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.DatosRecordatorio
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio de recordatorios: el ÚNICO punto de entrada a los datos para los
 * ViewModel (Módulo 3 · U1 · Tema 3).
 *
 * Contrato local-first:
 * - Las LECTURAS devuelven Flow y salen SIEMPRE de Room (fuente de verdad de
 *   la interfaz). Nunca esperan a la red.
 * - Las ESCRITURAS son suspend: se aplican primero en Room como PENDIENTE y
 *   después se programa la sincronización con el servicio remoto.
 * - Los ViewModel no saben si los datos vinieron de Room, del servidor o de
 *   una sincronización anterior, ni ven DTO, códigos HTTP o excepciones de red.
 */
interface RecordatorioRepository {

    /**
     * Listado para la pantalla principal, YA filtrado y ordenado según las
     * preferencias de DataStore (mostrar completados, filtro de prioridad,
     * orden). Coordina DataStore + Room.
     */
    fun observarListado(): Flow<List<Recordatorio>>

    /** Un recordatorio; emite null si no existe o fue eliminado. */
    fun observarRecordatorio(id: String): Flow<Recordatorio?>

    /** Cantidad de cambios locales que el servidor todavía no confirmó. */
    fun observarCambiosSinSincronizar(): Flow<Int>

    /** Estado de la última sincronización (para mostrar progreso o errores). */
    val estadoSincronizacion: StateFlow<EstadoSincronizacion>

    /** Lectura puntual (para precargar el formulario de edición). */
    suspend fun obtenerPorId(id: String): Recordatorio?

    /** Crea un recordatorio. Se guarda en Room como PENDIENTE aunque no haya conexión. */
    suspend fun crear(datos: DatosRecordatorio): Result<Recordatorio>

    /** Modifica los datos de un recordatorio existente (queda PENDIENTE). */
    suspend fun actualizar(id: String, datos: DatosRecordatorio): Result<Unit>

    /** Cambia el estado (pendiente, en curso, completado). */
    suspend fun cambiarEstado(id: String, estado: EstadoRecordatorio): Result<Unit>

    /** Elimina: se oculta enseguida y se borra del todo cuando el servidor confirma. */
    suspend fun eliminar(id: String): Result<Unit>

    /**
     * Ejecuta UNA sincronización completa (descargar, resolver conflictos y
     * subir pendientes). Está protegida con un Mutex: si se llama dos veces a
     * la vez, la segunda espera a que termine la primera.
     */
    suspend fun sincronizar(): ResultadoSincronizacion

    /** true si quedan cambios para enviar (PENDIENTE o PENDIENTE_ELIMINAR). */
    suspend fun hayCambiosPendientesDeEnvio(): Boolean

    /**
     * Pide una sincronización en segundo plano. Todos los disparadores
     * (guardar, volver la red, abrir la app, botón) pasan por acá y terminan en
     * UN trabajo único de WorkManager: varios pedidos simultáneos no generan
     * varias sincronizaciones.
     */
    fun solicitarSincronizacion()

    /** Sincronización manual: vuelve a poner en cola los registros con ERROR y la solicita. */
    suspend fun sincronizarManualmente()

    /** Categorías que se pueden elegir en el formulario. */
    fun categoriasDisponibles(): List<Categoria>
}

/**
 * Resultado de una sincronización completa (Módulo 3 · U2 · Tema 2, SyncResult).
 */
sealed interface ResultadoSincronizacion {
    /** Terminó bien. */
    data class Exito(val descargados: Int, val enviados: Int) : ResultadoSincronizacion

    /** Falla transitoria (sin red, timeout, 5xx): admite reintento automático con espera creciente. */
    data class ErrorReintentable(val motivo: String) : ResultadoSincronizacion

    /** El servidor rechazó datos (4xx) o respondió algo inválido: no se reintenta automáticamente. */
    data class ErrorPermanente(val motivo: String) : ResultadoSincronizacion
}

/** Estado observable de la sincronización, para la interfaz. */
sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Completada(val instante: Long) : EstadoSincronizacion
    data class Fallida(val mensaje: String, val seReintentara: Boolean) : EstadoSincronizacion
}

/**
 * Herramientas para PROBAR errores de almacenamiento local sin llenar el
 * disco de verdad (consigna: responder de forma controlada ante errores).
 */
interface ControlSimulacionLocal {
    fun fallarProximoGuardadoLocal()
}
