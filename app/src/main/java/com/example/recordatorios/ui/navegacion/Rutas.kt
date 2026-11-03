package com.example.recordatorios.ui.navegacion

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// RUTAS TIPADAS (Navigation Compose 2.8+). Cada destino es un tipo
// @Serializable: el compilador controla los argumentos, sin rutas escritas
// a mano como "detalle/{id}".
// ---------------------------------------------------------------------------

/** Destino inicial: listado. Sin argumentos → object. */
@Serializable
data object ListaRoute

/** Formulario de creación. Sin argumentos → object. */
@Serializable
data object CrearRoute

/** Detalle: transporta SOLO el identificador, no el objeto Recordatorio completo. */
@Serializable
data class DetalleRoute(val id: Long)
