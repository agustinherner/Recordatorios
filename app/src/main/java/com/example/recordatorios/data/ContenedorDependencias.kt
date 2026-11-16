package com.example.recordatorios.data

import android.content.Context
import com.example.recordatorios.BuildConfig
import com.example.recordatorios.data.conectividad.ObservadorConectividad
import com.example.recordatorios.data.local.AppDatabase
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.preferenciasDataStore
import com.example.recordatorios.data.remoto.ClienteRemoto
import com.example.recordatorios.data.remoto.ControlServidorSimulado
import com.example.recordatorios.data.remoto.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remoto.ServidorSimulado
import com.example.recordatorios.data.sincronizacion.ProgramadorSincronizacion
import java.io.File

/**
 * Contenedor manual de dependencias.
 *
 * Crea UNA sola instancia de cada pieza de la capa de datos (base Room,
 * DataStore, cliente Retrofit, repositorio) y la comparte con toda la app:
 * pantallas, ViewModel y el Worker de sincronización. Se inicializa una vez
 * en RecordatoriosApp.onCreate() con el contexto de la APLICACIÓN (nunca una
 * Activity, para no retenerla en memoria).
 *
 * Expone las piezas con el tipo de sus INTERFACES, no de las clases concretas.
 */
object ContenedorDependencias {

    private lateinit var contextoApp: Context

    fun inicializar(context: Context) {
        contextoApp = context.applicationContext
    }

    /** Observador de red compartido (lo usan el servidor simulado, los ViewModel y la app). */
    val conectividad: ObservadorConectividad by lazy { ObservadorConectividad(contextoApp) }

    /** Única instancia de Room. */
    private val baseDeDatos: AppDatabase by lazy { AppDatabase.crear(contextoApp) }

    /** Única instancia de DataStore, envuelta en su repositorio. */
    val preferencias: PreferenciasRepository by lazy { PreferenciasRepository(contextoApp.preferenciasDataStore) }

    /**
     * Servidor simulado: solo existe si API_SIMULADA=true (remote.properties).
     * Con false, Retrofit habla con el servicio real de API_BASE_URL.
     */
    private val servidorSimulado: ServidorSimulado? by lazy {
        if (BuildConfig.API_SIMULADA) {
            ServidorSimulado(
                archivo = File(contextoApp.filesDir, "servidor_simulado.json"),
                hayConexion = conectividad::estaConectado,
                json = ClienteRemoto.json
            )
        } else {
            null
        }
    }

    private val fuenteRemota: FuenteRemotaRecordatorios by lazy {
        FuenteRemotaRecordatorios(ClienteRemoto.crearApi(BuildConfig.API_BASE_URL, servidorSimulado))
    }

    private val repositorioLocalFirst: RecordatorioRepositoryLocalFirst by lazy {
        RecordatorioRepositoryLocalFirst(
            baseDeDatos = baseDeDatos,
            dao = baseDeDatos.recordatorioDao(),
            remota = fuenteRemota,
            preferencias = preferencias,
            programador = ProgramadorSincronizacion(contextoApp)
        )
    }

    val repositorio: RecordatorioRepository get() = repositorioLocalFirst

    /** Herramientas de prueba (menú ⋮ del listado). */
    val simulacionLocal: ControlSimulacionLocal get() = repositorioLocalFirst
    val controlServidor: ControlServidorSimulado? get() = servidorSimulado

    /** Para mostrar en la interfaz qué servicio se está usando. */
    val descripcionServicio: String
        get() = if (BuildConfig.API_SIMULADA) "Servidor simulado" else BuildConfig.API_BASE_URL
}
