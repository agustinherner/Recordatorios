package com.example.recordatorios.ui.crear

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.PRIORIDADES
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.validarCategoria
import com.example.recordatorios.domain.validarDescripcion
import com.example.recordatorios.domain.validarPrioridad
import com.example.recordatorios.domain.validarTitulo

/**
 * Estado del envío: alternativas EXCLUYENTES (sealed interface).
 * Evita combinaciones como "enviando = true y guardado = true".
 */
sealed interface EstadoEnvio {
    data object Editando : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Exito(val idCreado: Long) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

/**
 * Estado de la pantalla de creación (data class inmutable).
 *
 * Guarda solo los datos ORIGINALES (lo que escribió o eligió la persona).
 * Los mensajes de validación y la habilitación del botón se DERIVAN de esos
 * datos, así nunca pueden contradecirse (única fuente de verdad).
 */
data class CrearUiState(
    val titulo: String = "",
    val descripcion: String = "",
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
    val errorCategoria: String? get() = validarCategoria(categoria, categoriasDisponibles)
    val errorPrioridad: String? get() = validarPrioridad(prioridad)

    val formularioValido: Boolean
        get() = validarTitulo(titulo) == null && errorDescripcion == null &&
                errorCategoria == null && errorPrioridad == null

    val enviando: Boolean get() = envio is EstadoEnvio.Enviando

    /** El botón Guardar se habilita solo si el formulario es válido y no hay un envío en curso. */
    val puedeGuardar: Boolean get() = formularioValido && !enviando

    /** Lista de lo que falta, para explicar por qué el botón está deshabilitado. */
    val camposPendientes: List<String>
        get() = buildList {
            if (validarTitulo(titulo) != null) add("título válido")
            if (errorDescripcion != null) add("descripción más corta")
            if (errorCategoria != null) add("categoría")
            if (errorPrioridad != null) add("prioridad")
        }
}
