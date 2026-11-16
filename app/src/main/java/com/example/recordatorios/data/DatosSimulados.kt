package com.example.recordatorios.data

import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.OrdenListado
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.crearRecordatorio
import com.example.recordatorios.domain.estaPendiente
import com.example.recordatorios.domain.marcarCompletado

// ---------------------------------------------------------------------------
// DATOS DE EJEMPLO Y COLECCIONES (Módulo 1 · U2 · Tema 1: List, Set, Map,
// map/filter).
// Actividad 3: estos recordatorios ya NO son la fuente de la interfaz (ahora
// es Room). Se usan como contenido inicial del SERVIDOR SIMULADO, así la
// primera sincronización descarga datos remotos a Room, y en las previews.
// ---------------------------------------------------------------------------

object DatosSimulados {

    // Categorías definidas como constantes para reutilizarlas.
    val TRABAJO = Categoria("Trabajo")
    val ESTUDIO = Categoria("Estudio")
    val SALUD = Categoria("Salud")
    val HOGAR = Categoria("Hogar")
    val PERSONAL = Categoria("Personal")

    /** Opciones de categoría que ofrece el formulario (la validación exige que sea una de estas). */
    val categoriasDisponibles: List<Categoria> = listOf(TRABAJO, ESTUDIO, SALUD, HOGAR, PERSONAL)

    /**
     * MAP categoría → color (ARGB como Long).
     * Se guarda como Long y no como androidx.compose.ui.graphics.Color para
     * que este paquete siga sin depender de Compose; la UI hace la conversión.
     */
    val coloresPorCategoria: Map<Categoria, Long> = mapOf(
        TRABAJO to 0xFF1E88E5,   // azul
        ESTUDIO to 0xFF8E24AA,   // violeta
        SALUD to 0xFF43A047,     // verde
        HOGAR to 0xFFFB8C00,     // naranja
        PERSONAL to 0xFF00897B   // turquesa
    )

    /** MAP categoría → etiqueta corta para mostrar en un chip. */
    val etiquetasPorCategoria: Map<Categoria, String> = mapOf(
        TRABAJO to "TRAB",
        ESTUDIO to "EST",
        SALUD to "SAL",
        HOGAR to "HOG",
        PERSONAL to "PERS"
    )

    /** Color por defecto si una categoría no figura en el Map (map[k] devuelve null). */
    const val COLOR_POR_DEFECTO: Long = 0xFF757575

    fun colorDe(categoria: Categoria): Long = coloresPorCategoria[categoria] ?: COLOR_POR_DEFECTO
    fun etiquetaDe(categoria: Categoria): String = etiquetasPorCategoria[categoria] ?: categoria.nombre

    /**
     * Datos "crudos" tal como podrían llegar de un formulario.
     * Incluye DOS registros inválidos a propósito (título corto y fecha mal
     * escrita) para demostrar que la validación los descarta.
     */
    private val intentosDeCarga = listOf(
        crearRecordatorio("ejemplo-01", "Entregar Actividad 1 de Mobile", ESTUDIO, Prioridad.Alta,
            descripcion = "PDF con capturas y link al repositorio", fechaLimite = "2026-10-30",
            aviso = "1 día antes"),
        crearRecordatorio("ejemplo-02", "Reunión con proveedor de harina", TRABAJO, Prioridad.Alta,
            fechaLimite = "2026-10-24", ubicacion = "Depósito Toay", aviso = "30 min antes"),
        crearRecordatorio("ejemplo-03", "Turno control anual", SALUD, Prioridad.Media,
            fechaLimite = "2026-11-05", ubicacion = "Centro médico"),
        crearRecordatorio("ejemplo-04", "Pagar servicios del mes", HOGAR, Prioridad.Alta,
            fechaLimite = "2026-10-25"),
        crearRecordatorio("ejemplo-05", "Leer módulo 2 de Mobile", ESTUDIO, Prioridad.Media),
        crearRecordatorio("ejemplo-06", "Comprar regalo de cumpleaños", PERSONAL, Prioridad.Baja,
            descripcion = null),
        crearRecordatorio("ejemplo-07", "Revisar stock semanal", TRABAJO, Prioridad.Media,
            ubicacion = "Local Santa Rosa"),
        crearRecordatorio("ejemplo-08", "Ir al gimnasio", SALUD, Prioridad.Baja, aviso = "18:00"),
        crearRecordatorio("ejemplo-09", "Ok", PERSONAL, Prioridad.Baja),                       // inválido: título corto
        crearRecordatorio("ejemplo-10", "Renovar seguro del auto", HOGAR, Prioridad.Media,
            fechaLimite = "2026-13-40")                                                // inválido: fecha
    )

    /** Fecha fija (septiembre de 2026) para los ejemplos: cualquier cambio posterior de la persona es "más reciente". */
    const val MARCA_TIEMPO_EJEMPLOS: Long = 1_790_000_000_000L

    /** Mensajes de los registros descartados (para mostrarlos o registrarlos). */
    val erroresDeCarga: List<String> = intentosDeCarga
        .mapNotNull { it.exceptionOrNull()?.message }

    /**
     * LIST de recordatorios válidos. Se aplican cambios de estado con copy()
     * (inmutabilidad): no se modifica ningún objeto, se crean versiones nuevas.
     */
    val recordatorios: List<Recordatorio> = intentosDeCarga
        .mapNotNull { it.getOrNull() }
        .map { r ->
            when (r.id) {
                "ejemplo-05" -> r.copy(estado = EstadoRecordatorio.EnCurso)
                "ejemplo-06", "ejemplo-08" -> r.marcarCompletado("2026-10-20")
                else -> r
            }.copy(actualizadoEn = MARCA_TIEMPO_EJEMPLOS)
        }

    /**
     * SET de categorías usadas: aunque varias tareas compartan categoría,
     * el Set guarda cada una una sola vez (unicidad).
     */
    val categoriasUsadas: Set<Categoria> = recordatorios.map { it.categoria }.toSet()
}

// ---------------------------------------------------------------------------
// FILTROS: reciben una lista y devuelven OTRA lista; no modifican la original.
// ---------------------------------------------------------------------------

/** Filtra por estado. Se compara por tipo (::class) para que Completado(fecha) coincida con cualquier fecha. */
fun List<Recordatorio>.filtrarPorEstado(estado: EstadoRecordatorio): List<Recordatorio> =
    filter { it.estado::class == estado::class }

fun List<Recordatorio>.filtrarPorPrioridad(prioridad: Prioridad): List<Recordatorio> =
    filter { it.prioridad == prioridad }

fun List<Recordatorio>.soloPendientes(): List<Recordatorio> = filter { it.estaPendiente() }

/** Títulos de los pendientes: primero filter (cuáles) y después map (qué de cada uno). */
fun List<Recordatorio>.titulosPendientes(): List<String> = soloPendientes().map { it.titulo }

/** Orden para mostrar: pendientes primero y, dentro de ellos, mayor prioridad primero. */
fun List<Recordatorio>.ordenadosParaMostrar(): List<Recordatorio> =
    sortedWith(compareBy<Recordatorio> { !it.estaPendiente() }.thenByDescending { it.prioridad.nivel })

/**
 * Ordena según la preferencia guardada en DataStore (Actividad 3).
 * when exhaustivo sobre el enum: si se agrega un criterio nuevo, el
 * compilador obliga a contemplarlo acá.
 */
fun List<Recordatorio>.ordenadosPor(orden: OrdenListado): List<Recordatorio> = when (orden) {
    OrdenListado.PRIORIDAD -> ordenadosParaMostrar()
    // Sin fecha al final. "AAAA-MM-DD" se ordena bien como texto.
    OrdenListado.FECHA_LIMITE -> sortedWith(compareBy<Recordatorio> { it.fechaLimite == null }.thenBy { it.fechaLimite })
    OrdenListado.RECIENTES -> sortedByDescending { it.actualizadoEn }
    OrdenListado.ALFABETICO -> sortedBy { it.titulo.lowercase() }
}

/** MAP categoría → cantidad de recordatorios (agrupación). */
fun List<Recordatorio>.cantidadPorCategoria(): Map<Categoria, Int> =
    groupingBy { it.categoria }.eachCount()
