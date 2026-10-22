package com.example.recordatorios.domain

// ---------------------------------------------------------------------------
// MODELO DE DOMINIO (Módulo 1 · U2 · Tema 2: data classes, sealed classes e
// inmutabilidad). Este paquete NO depende de Android ni de Compose: se puede
// probar en la computadora sin emulador (Módulo 1 · U1 · Tema 3, "No todas las
// partes de la aplicación deben depender de Context").
// ---------------------------------------------------------------------------

/**
 * Categoría de un recordatorio.
 *
 * Es una data class (y no un String suelto) para que el compilador
 * distinga una categoría de cualquier otro texto. Al ser data class,
 * equals()/hashCode() comparan por valor: dos Categoria("Trabajo") son
 * iguales, y por eso un Set<Categoria> descarta repetidos.
 */
data class Categoria(val nombre: String)

/**
 * Prioridad del recordatorio: conjunto CERRADO de alternativas.
 *
 * Se usa sealed class porque solo existen tres prioridades y queremos que
 * un `when` sobre ella sea exhaustivo (sin `else`). Cada variante es un
 * `object` porque no necesita transportar datos propios: solo "es" esa
 * prioridad. El nivel numérico sirve para ordenar.
 */
sealed class Prioridad(val nivel: Int, val etiqueta: String) {
    object Alta : Prioridad(nivel = 3, etiqueta = "Alta")
    object Media : Prioridad(nivel = 2, etiqueta = "Media")
    object Baja : Prioridad(nivel = 1, etiqueta = "Baja")
}

/**
 * Estado del recordatorio.
 *
 * A diferencia de Prioridad, una variante transporta información propia:
 * Completado guarda la fecha en que se completó. Esto es lo que un enum
 * no permite y una sealed class sí (Módulo 1, Desafío Práctico — Cierre).
 */
sealed class EstadoRecordatorio {
    object Pendiente : EstadoRecordatorio()
    object EnCurso : EstadoRecordatorio()
    data class Completado(val fechaCompletado: String) : EstadoRecordatorio()
}

/**
 * Entidad principal: un recordatorio.
 *
 * - Todas las propiedades son `val` → el objeto es inmutable. Para
 *   "cambiarlo" se crea una nueva versión con copy().
 * - Los datos que pueden faltar se declaran anulables (`?`) con valor por
 *   defecto null (null safety, Módulo 1 · U2 · Tema 1).
 * - `ubicacion` y `aviso` anticipan lo que pide el contexto del proyecto
 *   (recordatorios asociados a una ubicación o un aviso) aunque en esta
 *   etapa son solo datos simulados.
 */
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
    val descripcion: String? = null,
    val fechaLimite: String? = null,   // formato "AAAA-MM-DD"
    val ubicacion: String? = null,
    val aviso: String? = null          // ej.: "30 min antes"
)

/**
 * Estado de la PANTALLA (no del recordatorio).
 *
 * Evita combinaciones contradictorias del tipo "cargando = true y además
 * error != null" (Módulo 1 · U2 · Tema 2, "Sealed classes e interfaces
 * selladas"). Cada estado lleva solo la información que necesita.
 */
sealed interface EstadoPantalla {
    object Cargando : EstadoPantalla
    data class ConDatos(val recordatorios: List<Recordatorio>) : EstadoPantalla
    object Vacio : EstadoPantalla
    data class Error(val mensaje: String) : EstadoPantalla
}

// ---------------------------------------------------------------------------
// FUNCIONES DE EXTENSIÓN (Módulo 1 · U2 · Tema 2). Agregan comportamiento al
// modelo sin modificar la clase y sin mutar el objeto original.
// ---------------------------------------------------------------------------

/** true si el recordatorio todavía no se completó. */
fun Recordatorio.estaPendiente(): Boolean = estado !is EstadoRecordatorio.Completado

/** Devuelve una NUEVA versión completada; el original no cambia (copy). */
fun Recordatorio.marcarCompletado(fecha: String): Recordatorio =
    copy(estado = EstadoRecordatorio.Completado(fecha))

/** Texto legible del estado, resuelto con un `when` exhaustivo (sin else). */
fun EstadoRecordatorio.etiqueta(): String = when (this) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    EstadoRecordatorio.EnCurso -> "En curso"
    is EstadoRecordatorio.Completado -> "Completado"   // smart cast disponible si hiciera falta la fecha
}
