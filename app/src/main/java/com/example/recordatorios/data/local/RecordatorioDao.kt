package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Operaciones permitidas sobre la tabla "recordatorios"
 * (Módulo 3 · U1 · Tema 2, "DAO y operaciones sobre los datos").
 *
 * Dos tipos de operación:
 * - OBSERVABLES (devuelven Flow): Room vuelve a ejecutar la consulta cada vez
 *   que cambia la tabla y emite el resultado nuevo. Las usa la interfaz.
 * - PUNTUALES (suspend): se ejecutan una vez. Las usan las escrituras y la
 *   sincronización.
 * Ninguna corre en el hilo principal: Room no lo permite.
 *
 * Los registros en PENDIENTE_ELIMINAR quedan OCULTOS para la interfaz: la
 * persona ya los eliminó, pero se conservan hasta que el servidor confirme.
 *
 * El DAO no decide mensajes de error ni navegación: solo accede a los datos.
 */
@Dao
interface RecordatorioDao {

    // ---------------------------------------------------------------------
    // Consultas observables (Flow) para la interfaz
    // ---------------------------------------------------------------------

    /** Listado completo visible (sin los eliminados pendientes de confirmar). */
    @Query("SELECT * FROM recordatorios WHERE estado_sync != 'PENDIENTE_ELIMINAR'")
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    /**
     * Listado con FILTROS que vienen de las preferencias de DataStore:
     * - mostrarCompletados = false oculta los de estado COMPLETADO (filtro por estado).
     * - prioridad = null no filtra; "ALTA"/"MEDIA"/"BAJA" deja solo esa (filtro por prioridad).
     * El filtro se aplica en SQL para no traer filas que no se van a mostrar.
     */
    @Query(
        """
        SELECT * FROM recordatorios
        WHERE estado_sync != 'PENDIENTE_ELIMINAR'
          AND (:mostrarCompletados = 1 OR estado != 'COMPLETADO')
          AND (:prioridad IS NULL OR prioridad = :prioridad)
        """
    )
    fun observarFiltrados(mostrarCompletados: Boolean, prioridad: String?): Flow<List<RecordatorioEntity>>

    /** Un recordatorio por id. Emite null si no existe o si se eliminó. */
    @Query("SELECT * FROM recordatorios WHERE id = :id AND estado_sync != 'PENDIENTE_ELIMINAR'")
    fun observarPorId(id: String): Flow<RecordatorioEntity?>

    /** Cantidad de cambios locales que todavía no confirmó el servidor (para el aviso de la interfaz). */
    @Query("SELECT COUNT(*) FROM recordatorios WHERE estado_sync != 'SINCRONIZADO'")
    fun observarCantidadSinSincronizar(): Flow<Int>

    // ---------------------------------------------------------------------
    // Lecturas puntuales (suspend)
    // ---------------------------------------------------------------------

    /** Incluye los PENDIENTE_ELIMINAR: lo usa la sincronización para comparar versiones. */
    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun obtenerPorId(id: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios")
    suspend fun obtenerTodos(): List<RecordatorioEntity>

    /**
     * "Fotografía" de lo que hay que enviar al servidor, del cambio más viejo
     * al más nuevo. No incluye ERROR: esos fueron rechazados (4xx) y
     * reenviarlos automáticamente daría el mismo resultado.
     */
    @Query(
        """
        SELECT * FROM recordatorios
        WHERE estado_sync IN ('PENDIENTE', 'PENDIENTE_ELIMINAR')
        ORDER BY actualizado_en ASC
        """
    )
    suspend fun obtenerPendientesDeEnvio(): List<RecordatorioEntity>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE estado_sync IN ('PENDIENTE', 'PENDIENTE_ELIMINAR')")
    suspend fun contarPendientesDeEnvio(): Int

    // ---------------------------------------------------------------------
    // Escrituras (suspend)
    // ---------------------------------------------------------------------

    /**
     * Inserta un recordatorio NUEVO. ABORT: si por error ya existiera ese id,
     * falla en lugar de pisar un registro (nunca debería pasar con UUID).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(recordatorio: RecordatorioEntity)

    /** Actualiza un recordatorio existente (edición, cambio de estado). */
    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    /** Inserta o reemplaza: se usa al incorporar los datos que llegan del servidor. */
    @Upsert
    suspend fun guardarTodos(recordatorios: List<RecordatorioEntity>)

    /** Elimina la fila definitivamente (después de que el servidor confirmó). */
    @Delete
    suspend fun eliminar(recordatorio: RecordatorioEntity)

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    /**
     * Marca un registro como SINCRONIZADO solo si NO cambió desde que se envió
     * (mismo actualizado_en). Si la persona lo editó mientras viajaba la
     * solicitud, queda PENDIENTE y se envía en la próxima sincronización:
     * así no se pierde la edición.
     * Devuelve la cantidad de filas modificadas (0 o 1).
     */
    @Query(
        """
        UPDATE recordatorios SET estado_sync = 'SINCRONIZADO'
        WHERE id = :id AND actualizado_en = :actualizadoEn
        """
    )
    suspend fun confirmarSincronizado(id: String, actualizadoEn: Long): Int

    /** Marca un registro rechazado por el servidor (4xx) para revisión. */
    @Query("UPDATE recordatorios SET estado_sync = 'ERROR' WHERE id = :id AND actualizado_en = :actualizadoEn")
    suspend fun marcarError(id: String, actualizadoEn: Long): Int

    /** Vuelve a poner en cola los registros con ERROR (reintento manual). */
    @Query("UPDATE recordatorios SET estado_sync = 'PENDIENTE' WHERE estado_sync = 'ERROR'")
    suspend fun reencolarErrores(): Int
}
