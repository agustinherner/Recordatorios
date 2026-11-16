package com.example.recordatorios.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.domain.FiltroPrioridad
import com.example.recordatorios.domain.OrdenListado
import com.example.recordatorios.domain.PreferenciasUsuario
import com.example.recordatorios.domain.TemaApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de Ajustes (Módulo 3 · U1 · Tema 1, "Edición de preferencias").
 *
 * - Lee: convierte el Flow de DataStore en StateFlow con stateIn, con
 *   PreferenciasUsuario() (valores predeterminados) como valor inicial.
 * - Escribe: cada cambio lanza una coroutine en viewModelScope que llama a una
 *   función suspendida del repositorio (dataStore.edit). No bloquea la interfaz.
 * No hace falta actualizar el estado a mano: DataStore emite el valor nuevo y
 * la pantalla se recompone sola.
 */
class AjustesViewModel(private val repositorio: PreferenciasRepository) : ViewModel() {

    val preferencias: StateFlow<PreferenciasUsuario> = repositorio.preferencias.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreferenciasUsuario()
    )

    fun onTemaChange(tema: TemaApp) {
        viewModelScope.launch { repositorio.cambiarTema(tema) }
    }

    fun onMostrarCompletadosChange(mostrar: Boolean) {
        viewModelScope.launch { repositorio.cambiarMostrarCompletados(mostrar) }
    }

    fun onOrdenChange(orden: OrdenListado) {
        viewModelScope.launch { repositorio.cambiarOrden(orden) }
    }

    fun onFiltroPrioridadChange(filtro: FiltroPrioridad) {
        viewModelScope.launch { repositorio.cambiarFiltroPrioridad(filtro) }
    }

    fun onRestablecer() {
        viewModelScope.launch { repositorio.restablecer() }
    }
}
