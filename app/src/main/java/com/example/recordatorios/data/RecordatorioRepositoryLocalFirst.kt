package com.example.recordatorios.data

import android.database.sqlite.SQLiteFullException
import android.util.Log
import androidx.room.withTransaction
import com.example.recordatorios.data.local.AppDatabase
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.remoto.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.data.remoto.ResultadoRemoto
import com.example.recordatorios.data.remoto.descripcion
import com.example.recordatorios.data.remoto.esReintentable
import com.example.recordatorios.data.sincronizacion.DecisionFusion
import com.example.recordatorios.data.sincronizacion.ProgramadorSincronizacion
import com.example.recordatorios.data.sincronizacion.decidirFusion
import com.example.recordatorios.data.sincronizacion.idsEliminadosEnServidor
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.DatosRecordatorio
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.ResultadoValidacion
import com.example.recordatorios.domain.crearRecordatorio
import com.example.recordatorios.domain.validarRecordatorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación LOCAL-FIRST del repositorio (Módulo 3 · U1 · Tema 3 y U2).
 *
 * Coordina las tres fuentes:
 *  - DataStore (PreferenciasRepository): cómo quiere ver el listado la persona.
 *  - Room (RecordatorioDao): los recordatorios. FUENTE DE VERDAD de la interfaz.
 *  - Servicio remoto (FuenteRemotaRecordatorios): copia compartida en el servidor.
 *
 *   Compose ─evento─► ViewModel ─► Repositorio ─escribe─► Room ─Flow─► ViewModel ─► Compose
 *                                       │                    ▲
 *                                       └─ WorkManager ─► sincronizar() ◄─► Servicio remoto
 *
 * La red NUNCA alimenta la pantalla directamente: lo que llega del servidor
 * se transforma y se guarda en Room, y la pantalla se entera por el Flow.
 */
class RecordatorioRepositoryLocalFirst(
    private val baseDeDatos: AppDatabase,
    private val dao: RecordatorioDao,
    private val remota: FuenteRemotaRecordatorios,
    private val preferencias: PreferenciasRepository,
    private val programador: ProgramadorSincronizacion,
    private val reloj: () -> Long = System::currentTimeMillis
) : RecordatorioRepository, ControlSimulacionLocal {

    /** Garantiza que UNA sola sincronización corra a la vez dentro del proceso. */
    private val mutexSincronizacion = Mutex()

    private val _estadoSincronizacion = MutableStateFlow<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
    override val estadoSincronizacion: StateFlow<EstadoSincronizacion> = _estadoSincronizacion.asStateFlow()

    @Volatile
    private var fallarGuardadoLocal = false

    // =========================================================================
    // LECTURAS: siempre desde Room
    // =========================================================================

    /**
     * Cada vez que cambian las preferencias, flatMapLatest cancela la consulta
     * anterior y abre una nueva con los filtros actualizados. Cada vez que
     * cambia la tabla, Room vuelve a emitir. El orden se aplica en memoria.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarListado(): Flow<List<Recordatorio>> =
        preferencias.preferencias.flatMapLatest { prefs ->
            dao.observarFiltrados(
                mostrarCompletados = prefs.mostrarCompletados,
                prioridad = prefs.filtroPrioridad.prioridad?.codigo
            ).map { filas -> filas.map { it.aDominio() }.ordenadosPor(prefs.orden) }
        }

    override fun observarRecordatorio(id: String): Flow<Recordatorio?> =
        dao.observarPorId(id).map { it?.aDominio() }

    override fun observarCambiosSinSincronizar(): Flow<Int> = dao.observarCantidadSinSincronizar()

    override suspend fun obtenerPorId(id: String): Recordatorio? =
        dao.obtenerPorId(id)?.takeIf { it.estadoSync != EstadoSync.PENDIENTE_ELIMINAR }?.aDominio()

    // =========================================================================
    // ESCRITURAS: primero Room (PENDIENTE), después sincronización
    // =========================================================================

    override suspend fun crear(datos: DatosRecordatorio): Result<Recordatorio> = operacionLocal("crear") {
        val nuevo = crearRecordatorio(
            id = UUID.randomUUID().toString(),   // id ESTABLE desde el primer momento, aun sin red
            titulo = datos.titulo,
            categoria = datos.categoria,
            prioridad = datos.prioridad,
            descripcion = datos.descripcion,
            fechaLimite = datos.fechaLimite,
            actualizadoEn = reloj(),
            estadoSync = EstadoSync.PENDIENTE
        ).getOrThrow()                          // datos inválidos → IllegalArgumentException
        dao.insertar(nuevo.aEntity())
        nuevo
    }

    override suspend fun actualizar(id: String, datos: DatosRecordatorio): Result<Unit> =
        modificar("actualizar", id) { actual ->
            actual.copy(
                titulo = datos.titulo.trim(),
                descripcion = datos.descripcion?.trim()?.takeIf { it.isNotEmpty() },
                categoria = datos.categoria,
                prioridad = datos.prioridad,
                fechaLimite = datos.fechaLimite?.trim()?.takeIf { it.isNotEmpty() }
            )
        }

    override suspend fun cambiarEstado(id: String, estado: EstadoRecordatorio): Result<Unit> =
        modificar("cambiar estado", id) { actual -> actual.copy(estado = estado) }

    override suspend fun eliminar(id: String): Result<Unit> = operacionLocal("eliminar") {
        val actual = dao.obtenerPorId(id) ?: throw NoSuchElementException("No existe $id")
        // No se borra todavía: queda oculto hasta que el servidor confirme la eliminación.
        dao.actualizar(actual.copy(actualizadoEn = reloj(), estadoSync = EstadoSync.PENDIENTE_ELIMINAR))
    }

    /** Lee, aplica el cambio, VALIDA y guarda como PENDIENTE con nueva marca de tiempo. */
    private suspend fun modificar(
        descripcion: String,
        id: String,
        cambio: (Recordatorio) -> Recordatorio
    ): Result<Unit> = operacionLocal(descripcion) {
        val actual = dao.obtenerPorId(id)
            ?.takeIf { it.estadoSync != EstadoSync.PENDIENTE_ELIMINAR }
            ?.aDominio()
            ?: throw NoSuchElementException("No existe $id")
        val modificado = cambio(actual).copy(actualizadoEn = reloj(), estadoSync = EstadoSync.PENDIENTE)
        val validacion = validarRecordatorio(modificado)
        if (validacion is ResultadoValidacion.Invalido) {
            throw IllegalArgumentException(validacion.errores.joinToString("; "))
        }
        dao.actualizar(modificado.aEntity())
    }

    /**
     * Envoltorio común de las escrituras locales (Módulo 3 · U1 · Tema 4):
     * - devuelve Result en lugar de lanzar la excepción a la interfaz;
     * - los errores de validación quedan como IllegalArgumentException;
     * - los de almacenamiento se clasifican (sin espacio, datos dañados, …) y
     *   el detalle técnico se registra en Logcat;
     * - si la escritura salió bien, se solicita la sincronización.
     */
    private suspend fun <T> operacionLocal(descripcion: String, bloque: suspend () -> T): Result<T> =
        try {
            if (fallarGuardadoLocal) {
                fallarGuardadoLocal = false
                throw SQLiteFullException("Falta de espacio SIMULADA para pruebas")
            }
            val resultado = bloque()
            solicitarSincronizacion()
            Result.success(resultado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Falló la operación local \"$descripcion\"", e)
            Result.failure(ErrorAlmacenamientoException(e.aErrorAlmacenamiento(), e))
        }

    override fun fallarProximoGuardadoLocal() {
        fallarGuardadoLocal = true
    }

    // =========================================================================
    // SINCRONIZACIÓN
    // =========================================================================

    override fun solicitarSincronizacion() = programador.programar()

    override suspend fun hayCambiosPendientesDeEnvio(): Boolean = dao.contarPendientesDeEnvio() > 0

    override suspend fun sincronizarManualmente() {
        dao.reencolarErrores()   // la persona pide reintentar también lo rechazado antes
        programador.programar(inmediato = true)
    }

    /**
     * Una sincronización completa, en este orden:
     *  1. DESCARGAR la versión remota.
     *  2. FUSIONAR en Room resolviendo conflictos (last-write-wins). Se hace
     *     antes de subir para no pisar en el servidor un cambio remoto más nuevo.
     *  3. SUBIR los cambios locales que quedaron pendientes.
     *
     * withLock: si WorkManager y otro componente la piden a la vez, se ejecutan
     * una después de la otra, nunca en paralelo (evita envíos duplicados).
     */
    override suspend fun sincronizar(): ResultadoSincronizacion = mutexSincronizacion.withLock {
        _estadoSincronizacion.value = EstadoSincronizacion.EnCurso
        val resultado = try {
            ejecutarSincronizacion()
        } catch (e: CancellationException) {
            _estadoSincronizacion.value = EstadoSincronizacion.Inactiva
            throw e
        } catch (e: Exception) {
            // Error local (por ejemplo, de Room) durante la sincronización.
            Log.e(TAG, "Error inesperado durante la sincronización", e)
            ResultadoSincronizacion.ErrorReintentable("error local: ${e::class.simpleName}")
        }
        _estadoSincronizacion.value = when (resultado) {
            is ResultadoSincronizacion.Exito -> EstadoSincronizacion.Completada(reloj())
            is ResultadoSincronizacion.ErrorReintentable ->
                EstadoSincronizacion.Fallida(mensajeReintentable(resultado.motivo), seReintentara = true)
            is ResultadoSincronizacion.ErrorPermanente ->
                EstadoSincronizacion.Fallida(resultado.motivo, seReintentara = false)
        }
        Log.i(TAG, "Sincronización: $resultado")
        resultado
    }

    private suspend fun ejecutarSincronizacion(): ResultadoSincronizacion {
        // 1. Descargar
        val descarga = remota.listar()
        if (descarga !is ResultadoRemoto.Exito) return descarga.aResultadoSincronizacion()

        // 2. Fusionar
        val descargados = fusionarCambiosRemotos(descarga.datos)

        // 3. Subir
        var enviados = 0
        var rechazados = 0
        for (pendiente in dao.obtenerPendientesDeEnvio()) {
            val eliminacion = pendiente.estadoSync == EstadoSync.PENDIENTE_ELIMINAR
            val respuesta = if (eliminacion) remota.eliminar(pendiente.id) else remota.guardar(pendiente.aDto())

            when {
                respuesta is ResultadoRemoto.Exito -> {
                    if (eliminacion) dao.eliminarPorId(pendiente.id)
                    // Solo se confirma si nadie lo modificó mientras viajaba la solicitud.
                    else dao.confirmarSincronizado(pendiente.id, pendiente.actualizadoEn)
                    enviados++
                }
                // Falla transitoria: se corta acá. Lo que falta sigue PENDIENTE y se
                // reintenta después; como el envío es idempotente, no se duplica.
                respuesta.esReintentable() -> return respuesta.aResultadoSincronizacion()
                // Rechazo (4xx) o respuesta inválida: reintentarlo daría lo mismo.
                else -> {
                    Log.w(TAG, "El servidor rechazó un cambio: ${respuesta.descripcion()}")
                    if (!eliminacion) dao.marcarError(pendiente.id, pendiente.actualizadoEn)
                    rechazados++
                }
            }
        }

        return if (rechazados > 0) {
            ResultadoSincronizacion.ErrorPermanente(
                "El servidor rechazó $rechazados cambio(s). Revisalos y usá \"Sincronizar ahora\" para reintentar."
            )
        } else {
            ResultadoSincronizacion.Exito(descargados = descargados, enviados = enviados)
        }
    }

    /**
     * Incorpora la versión remota a Room dentro de UNA transacción (todo o nada).
     * Devuelve cuántos registros se escribieron.
     */
    private suspend fun fusionarCambiosRemotos(dtos: List<RecordatorioDto>): Int =
        baseDeDatos.withTransaction {
            val remotos = dtos.mapNotNull { dto ->
                dto.aEntityOrNull() ?: run {
                    Log.w(TAG, "Se descartó un recordatorio remoto inválido (id vacío o sin título)")
                    null
                }
            }
            val locales = dao.obtenerTodos().associateBy { it.id }

            val aGuardar = mutableListOf<RecordatorioEntity>()
            remotos.forEach { remoto ->
                when (val decision = decidirFusion(locales[remoto.id], remoto)) {
                    is DecisionFusion.GuardarRemoto -> aGuardar += decision.recordatorio
                    DecisionFusion.ConservarLocal, DecisionFusion.SinCambios -> Unit
                }
            }
            if (aGuardar.isNotEmpty()) dao.guardarTodos(aGuardar)

            // Eliminados desde otro dispositivo. Se usan TODOS los ids recibidos
            // (también los inválidos) para no borrar por error una copia local.
            val idsRemotos = dtos.map { it.id }.toSet()
            val eliminados = idsEliminadosEnServidor(locales.values, idsRemotos)
            eliminados.forEach { dao.eliminarPorId(it) }

            aGuardar.size + eliminados.size
        }

    private fun ResultadoRemoto<*>.aResultadoSincronizacion(): ResultadoSincronizacion =
        if (esReintentable()) ResultadoSincronizacion.ErrorReintentable(descripcion())
        else ResultadoSincronizacion.ErrorPermanente("No se pudo sincronizar: ${descripcion()}.")

    private fun mensajeReintentable(motivo: String): String = when (motivo) {
        "sin conexión" -> "Sin conexión. Los cambios se sincronizarán cuando vuelva la red."
        "timeout" -> "El servidor no respondió a tiempo. Se reintentará automáticamente."
        else -> "No se pudo sincronizar ($motivo). Se reintentará automáticamente."
    }

    override fun categoriasDisponibles(): List<Categoria> = DatosSimulados.categoriasDisponibles

    private companion object {
        const val TAG = "Repositorio"
    }
}
