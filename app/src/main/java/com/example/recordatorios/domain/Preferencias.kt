package com.example.recordatorios.domain

// ---------------------------------------------------------------------------
// MODELO DE PREFERENCIAS (Módulo 3 · U1 · Tema 1, "Claves y modelo de
// preferencias"). DataStore NO guarda esta clase: guarda pares clave-valor y
// el repositorio de preferencias los transforma en este objeto. Así el resto
// de la app trabaja con propiedades claras sin conocer los nombres de las claves.
// ---------------------------------------------------------------------------

/** Tema visual elegido por la persona. */
enum class TemaApp(val etiqueta: String) {
    SISTEMA("Según el sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

/** Criterio de ordenamiento del listado. */
enum class OrdenListado(val etiqueta: String) {
    PRIORIDAD("Prioridad"),
    FECHA_LIMITE("Fecha límite"),
    RECIENTES("Modificados recientemente"),
    ALFABETICO("Título (A-Z)")
}

/** Filtro de prioridad del listado. */
enum class FiltroPrioridad(val etiqueta: String, val prioridad: Prioridad?) {
    TODAS("Todas", null),
    ALTA("Alta", Prioridad.Alta),
    MEDIA("Media", Prioridad.Media),
    BAJA("Baja", Prioridad.Baja)
}

/**
 * Preferencias de la app con sus VALORES PREDETERMINADOS.
 * Se usan cuando todavía no se guardó nada, cuando el valor guardado es
 * desconocido o cuando falla la lectura (IOException).
 */
data class PreferenciasUsuario(
    val tema: TemaApp = TemaApp.SISTEMA,
    val mostrarCompletados: Boolean = true,
    val orden: OrdenListado = OrdenListado.PRIORIDAD,
    val filtroPrioridad: FiltroPrioridad = FiltroPrioridad.TODAS
)

/**
 * Busca la constante del enum cuyo nombre coincide con [valor].
 * Si el valor no existe (dato inválido o de una versión vieja) devuelve null
 * y quien la llama aplica el valor predeterminado.
 */
inline fun <reified T : Enum<T>> enumDesdeTexto(valor: String?): T? =
    enumValues<T>().firstOrNull { it.name == valor }
