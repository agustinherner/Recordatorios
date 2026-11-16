package com.example.recordatorios.data.sincronizacion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Programa la sincronización como TRABAJO ÚNICO de WorkManager
 * (Módulo 3 · U2 · Tema 2, "Introducción a WorkManager" y "Prevención de trabajos duplicados").
 *
 * Problema de la consigna: al volver la conexión, varios eventos intentaban
 * sincronizar a la vez y se enviaban operaciones repetidas. Solución:
 * - Todos los disparadores llaman a programar().
 * - enqueueUniqueWork con un NOMBRE fijo y ExistingWorkPolicy.KEEP: si ya hay
 *   una sincronización encolada o en curso, el pedido nuevo se descarta.
 *   Diez eventos seguidos producen UNA sola sincronización.
 * - Restricción CONNECTED: si no hay red, el trabajo espera y WorkManager lo
 *   ejecuta cuando vuelve la conectividad, aunque la app esté cerrada.
 * - Backoff EXPONENCIAL: si el trabajo pide reintento (timeout, 5xx), espera
 *   30 s, luego 60 s, 120 s… para no saturar el servidor.
 *
 * Excepción: el botón "Sincronizar ahora" usa REPLACE. Si había un trabajo
 * esperando su próximo reintento, lo reemplaza por uno que corre ya. Sigue
 * habiendo UN solo trabajo con ese nombre.
 */
class ProgramadorSincronizacion(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** @param inmediato true solo para la sincronización manual (REPLACE en lugar de KEEP). */
    fun programar(inmediato: Boolean = false) {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val solicitud = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(restricciones)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val politica = if (inmediato) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(NOMBRE_TRABAJO, politica, solicitud)
    }

    companion object {
        const val NOMBRE_TRABAJO = "sincronizacion_recordatorios"
    }
}
