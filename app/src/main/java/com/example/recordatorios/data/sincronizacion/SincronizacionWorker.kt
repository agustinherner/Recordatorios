package com.example.recordatorios.data.sincronizacion

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.recordatorios.data.ContenedorDependencias
import com.example.recordatorios.data.ResultadoSincronizacion

/**
 * Trabajo de sincronización en segundo plano (Módulo 3 · U2 · Tema 2).
 *
 * CoroutineWorker: doWork() es suspend, así que llama al repositorio sin
 * bloquear hilos. Traduce el resultado de la sincronización al resultado
 * que entiende WorkManager:
 *
 * | ResultadoSincronizacion | Result de WorkManager                         |
 * |-------------------------|-----------------------------------------------|
 * | Exito                   | success()  (o retry() si llegaron cambios nuevos mientras corría) |
 * | ErrorReintentable       | retry()    → aplica el backoff exponencial     |
 * | ErrorPermanente         | failure()  → no se reintenta automáticamente  |
 *
 * Los reintentos tienen LÍMITE: después de [MAX_INTENTOS] se termina con
 * failure(). Los cambios siguen PENDIENTE en Room y se envían en la próxima
 * sincronización (al guardar, al volver la red, al abrir la app o con el botón).
 */
class SincronizacionWorker(
    appContext: Context,
    parametros: WorkerParameters
) : CoroutineWorker(appContext, parametros) {

    override suspend fun doWork(): Result {
        val repositorio = ContenedorDependencias.repositorio
        val resultado = repositorio.sincronizar()
        Log.i(TAG, "Intento ${runAttemptCount + 1}: $resultado")

        return when (resultado) {
            is ResultadoSincronizacion.Exito ->
                // Un cambio hecho DURANTE la sincronización queda PENDIENTE (no se pierde):
                // se pide otra pasada en lugar de esperar al próximo disparador.
                if (repositorio.hayCambiosPendientesDeEnvio()) Result.retry() else Result.success()

            is ResultadoSincronizacion.ErrorReintentable ->
                if (runAttemptCount + 1 >= MAX_INTENTOS) Result.failure() else Result.retry()

            is ResultadoSincronizacion.ErrorPermanente -> Result.failure()
        }
    }

    private companion object {
        const val TAG = "SincronizacionWorker"
        const val MAX_INTENTOS = 5
    }
}
