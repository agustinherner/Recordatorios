package com.example.recordatorios.ui.navegacion

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// RUTAS TIPADAS (Navigation Compose 2.8+). Cada destino es un tipo
// @Serializable: el compilador controla los argumentos.
// Actividad 3: el id pasa a ser String (UUID estable) y se agregan las rutas
// de edición y de ajustes.
// ---------------------------------------------------------------------------

/** Destino inicial: listado. */
@Serializable
data object ListaRoute

/** Formulario: idEditar = null crea un recordatorio nuevo; con valor, edita ese recordatorio. */
@Serializable
data class FormularioRoute(val idEditar: String? = null)

/** Detalle: transporta SOLO el identificador, no el objeto Recordatorio completo. */
@Serializable
data class DetalleRoute(val id: String)

/** Preferencias guardadas con DataStore. */
@Serializable
data object AjustesRoute
