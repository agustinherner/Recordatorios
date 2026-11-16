package com.example.recordatorios

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.recordatorios.data.ContenedorDependencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Clase Application: se crea antes que cualquier Activity o Worker.
 *
 * - Inicializa el contenedor de dependencias.
 * - Registra los DISPARADORES de sincronización. Todos terminan en
 *   solicitarSincronizacion() → un único trabajo de WorkManager, así que
 *   aunque coincidan (la app vuelve a primer plano justo cuando vuelve la
 *   red) no se generan sincronizaciones duplicadas.
 */
class RecordatoriosApp : Application() {

    /** Alcance de toda la app: vive mientras vive el proceso. */
    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ContenedorDependencias.inicializar(this)
        val repositorio = ContenedorDependencias.repositorio

        // Disparador 1: la app vuelve a primer plano (también al abrirla).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                repositorio.solicitarSincronizacion()
            }
        })

        // Disparador 2: se recupera la conectividad.
        // drop(1) ignora el valor inicial; filter deja pasar solo los "volvió la red".
        alcanceApp.launch {
            ContenedorDependencias.conectividad.conectado
                .drop(1)
                .filter { conectado -> conectado }
                .collect { repositorio.solicitarSincronizacion() }
        }
    }
}
