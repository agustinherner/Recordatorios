package com.example.recordatorios.ui.crear

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.PRIORIDADES
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.validarCategoria
import com.example.recordatorios.domain.validarDescripcion
import com.example.recordatorios.domain.validarFechaLimite
import com.example.recordatorios.domain.validarPrioridad
import com.example.recordatorios.domain.validarTitulo

/**
 * Estado del envío: alternativas EXCLUYENTES (sealed interface).
 */
sealed interface EstadoEnvio {
    data object Editando : EstadoEnvio
    data object Enviando : EstadoEnvio
    data object Exito : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

/**
 * Estado del formulario (crear o editar).
 *
 * Guarda solo los datos ORIGINALES (lo que escribió o eligió la persona).
 * Los mensajes de validación y la habilitación del botón se DERIVAN de esos
 * datos, así nunca pueden contradecirse (única fuente de verdad).
 */
data class CrearUiState(
    val modoEdicion: Boolean = false,
    /** En edición: true mientras se cargan los datos actuales del recordatorio. */
    val cargandoDatos: Boolean = false,
    /** En edición: mensaje si el recordatorio no existe o no se pudo leer. */
    val errorCarga: String? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val fechaLimite: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val categoriasDisponibles: List<Categoria> = emptyList(),
    val prioridadesDisponibles: List<Prioridad> = PRIORIDADES,
    // El error del título se muestra recién cuando la persona empezó a escribir.
    val tituloTocado: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Editando
) {
    val errorTitulo: String? get() = if (tituloTocado) validarTitulo(titulo) else null
    val errorDescripcion: String? get() = validarDescripcion(descripcion)
    val errorFecha: String? get() = validarFechaLimite(fechaLimite)
    val errorCategoria: String? get() = validarCategoria(categoria, categoriasDisponibles)
    val errorPrioridad: String? get() = validarPrioridad(prioridad)

    val formularioValido: Boolean
        get() = validarTitulo(titulo) == null && errorDescripcion == null && errorFecha == null &&
                errorCategoria == null && errorPrioridad == null

    val enviando: Boolean get() = envio is EstadoEnvio.Enviando

    /** El botón Guardar se habilita solo si el formulario es válido, terminó de cargar y no hay un envío en curso. */
    val puedeGuardar: Boolean get() = formularioValido && !enviando && !cargandoDatos && errorCarga == null

    /** Lista de lo que falta, para explicar por qué el botón está deshabilitado. */
    val camposPendientes: List<String>
        get() = buildList {
            if (validarTitulo(titulo) != null) add("título válido")
            if (errorDescripcion != null) add("descripción más corta")
            if (errorFecha != null) add("fecha válida")
            if (errorCategoria != null) add("categoría")
            if (errorPrioridad != null) add("prioridad")
        }
}
