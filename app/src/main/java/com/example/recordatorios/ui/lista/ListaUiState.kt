package com.example.recordatorios.ui.lista

import com.example.recordatorios.domain.PreferenciasUsuario
import com.example.recordatorios.domain.Recordatorio

/**
 * Estado de la pantalla de listado (Actividad 3).
 *
 * Pasa de sealed interface a data class porque ahora hay información que
 * CONVIVE: se pueden mostrar los recordatorios guardados Y, al mismo tiempo,
 * el aviso de "sin conexión" o "sincronizando". En local-first la falta de
 * red no reemplaza el contenido por un error.
 *
 * "No hay recordatorios" (lista vacía) y "no se pudieron leer" (errorLectura)
 * son estados DISTINTOS y se comunican distinto (Módulo 3 · U1 · Tema 4).
 */
data class ListaUiState(
    val cargando: Boolean = true,
    val recordatorios: List<Recordatorio> = emptyList(),
    val errorLectura: String? = null,
    val preferencias: PreferenciasUsuario = PreferenciasUsuario(),
    val enLinea: Boolean = true,
    val cambiosSinSincronizar: Int = 0,
    val sincronizando: Boolean = false,
    val mensajeSincronizacion: String? = null,
    val mensajeSincronizacionEsError: Boolean = false,
    val servicio: String = ""
) {
    /** true si algún filtro de las preferencias puede estar ocultando recordatorios. */
    val hayFiltrosActivos: Boolean
        get() = !preferencias.mostrarCompletados || preferencias.filtroPrioridad.prioridad != null

    /** Resumen de las preferencias aplicadas, para que se entienda por qué la lista se ve así. */
    val resumenPreferencias: String
        get() = buildList {
            add("Orden: ${preferencias.orden.etiqueta}")
            if (!preferencias.mostrarCompletados) add("ocultando completados")
            preferencias.filtroPrioridad.prioridad?.let { add("solo prioridad ${it.etiqueta}") }
        }.joinToString(" · ")
}

/** Opciones del menú de pruebas, para provocar cada situación de error sin modificar el código. */
enum class OpcionSimulacion(val texto: String, val requiereServidorSimulado: Boolean) {
    SERVIDOR_ERROR_500("Servidor: error 500 en la próxima solicitud", true),
    SERVIDOR_ERROR_400("Servidor: error 400 en la próxima solicitud", true),
    SERVIDOR_TIMEOUT("Servidor: timeout en la próxima solicitud", true),
    CAMBIO_OTRO_DISPOSITIVO("Simular cambio desde otro dispositivo", true),
    ERROR_GUARDADO_LOCAL("Simular falta de espacio en el próximo guardado", false),
    PREFERENCIA_INVALIDA("Escribir preferencias inválidas", false)
}
