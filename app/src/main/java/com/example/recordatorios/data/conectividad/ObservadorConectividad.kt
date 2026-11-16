package com.example.recordatorios.data.conectividad

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observa el estado de la red (Módulo 3 · U2 · Tema 2, "Observación de la conectividad mediante Flow").
 *
 * IMPORTANTE: la conectividad es una SEÑAL del entorno, no una garantía. Puede
 * haber Wi-Fi sin internet o perderse la señal en medio de una solicitud; por
 * eso la fuente remota igual controla timeouts y errores en cada llamada.
 *
 * Requiere el permiso ACCESS_NETWORK_STATE (manifiesto). No se pide en tiempo
 * de ejecución porque no accede a información personal.
 */
class ObservadorConectividad(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    /** Consulta puntual: ¿la red activa declara acceso a internet? */
    fun estaConectado(): Boolean {
        val red = connectivityManager.activeNetwork ?: return false
        val capacidades = connectivityManager.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Flujo que emite true/false cada vez que cambia la conectividad.
     * callbackFlow convierte la API de callbacks de Android en un Flow;
     * awaitClose DESREGISTRA el callback cuando ya nadie observa, para no
     * acumular callbacks ni gastar recursos.
     */
    val conectado: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                // Puede haberse perdido una red y seguir otra activa: se vuelve a consultar.
                trySend(estaConectado())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }

        trySend(estaConectado())   // valor inicial
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
