package com.example.recordatorios.domain

// ---------------------------------------------------------------------------
// MODELO DE DOMINIO (Módulo 1 · U2 · Tema 2: data classes, sealed classes e
// inmutabilidad). Este paquete NO depende de Android, de Room ni de Retrofit:
// es el modelo que usa el resto de la app (Módulo 3 · U1 · Tema 3, "Modelos y
// contrato del repositorio"). Room usa RecordatorioEntity y el servicio usa
// RecordatorioDto; los mapeos viven en data/Mapeos.kt.
// ---------------------------------------------------------------------------

/**
 * Categoría de un recordatorio.
 *
 * Es una data class (y no un String suelto) para que el compilador
 * distinga una categoría de cualquier otro texto. Al ser data class,
 * equals()/hashCode() comparan por valor.
 */
data class Categoria(val nombre: String)

/**
 * Prioridad del recordatorio: conjunto CERRADO de alternativas.
 *
 * [codigo] es el valor ESTABLE que se guarda en Room y viaja al servicio.
 * Se usa un código propio y no el nombre de la clase porque, si un día se
 * renombra la variante, los datos ya guardados seguirían siendo legibles
 * (Módulo 3 · U1 · Tema 2, "Conversión de tipos").
 */
sealed class Prioridad(val nivel: Int, val etiqueta: String, val codigo: String) {
    object Alta : Prioridad(nivel = 3, etiqueta = "Alta", codigo = "ALTA")
    object Media : Prioridad(nivel = 2, etiqueta = "Media", codigo = "MEDIA")
    object Baja : Prioridad(nivel = 1, etiqueta = "Baja", codigo = "BAJA")

    companion object {
        /** Devuelve la prioridad de un código, o null si el código es desconocido. */
        fun desdeCodigo(codigo: String?): Prioridad? = PRIORIDADES.find { it.codigo == codigo }
    }
}

/**
 * Estado del recordatorio. Completado transporta la fecha en que se completó
 * (lo que un enum no permite y una sealed class sí).
 */
sealed class EstadoRecordatorio(val codigo: String) {
    object Pendiente : EstadoRecordatorio("PENDIENTE")
    object EnCurso : EstadoRecordatorio("EN_CURSO")
    data class Completado(val fechaCompletado: String) : EstadoRecordatorio("COMPLETADO")
}

/**
 * Estado de SINCRONIZACIÓN de un registro con el servicio remoto
 * (Módulo 3 · U1 · Tema 3, "Guardado y sincronización").
 *
 * - SINCRONIZADO: la versión local coincide con la del servidor.
 * - PENDIENTE: creado o modificado sin conexión; falta enviarlo.
 * - PENDIENTE_ELIMINAR: se eliminó localmente; se conserva oculto hasta que
 *   el servidor confirme la eliminación.
 * - ERROR: el servidor rechazó la operación (4xx); necesita revisión.
 *
 * [codigo] es el texto estable guardado en la columna estado_sync.
 */
enum class EstadoSync(val codigo: String) {
    SINCRONIZADO("SINCRONIZADO"),
    PENDIENTE("PENDIENTE"),
    PENDIENTE_ELIMINAR("PENDIENTE_ELIMINAR"),
    ERROR("ERROR");

    companion object {
        /** Código desconocido → PENDIENTE: ante la duda, se vuelve a enviar (la operación es idempotente). */
        fun desdeCodigo(codigo: String?): EstadoSync = entries.find { it.codigo == codigo } ?: PENDIENTE
    }
}

/**
 * Entidad principal del dominio: un recordatorio.
 *
 * - [id] es un UUID generado en el dispositivo al crear el registro. Es
 *   ESTABLE: el mismo valor identifica el recordatorio en Room y en el
 *   servidor, así un reintento actualiza el mismo recurso en lugar de crear
 *   uno nuevo (estrategia para evitar duplicados).
 * - [actualizadoEn] es la marca de tiempo de la última modificación, usada
 *   para resolver conflictos (last-write-wins).
 * - [estadoSync] le permite a la interfaz mostrar si el cambio ya fue
 *   confirmado por el servicio.
 */
data class Recordatorio(
    val id: String,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
    val descripcion: String? = null,
    val fechaLimite: String? = null,   // formato "AAAA-MM-DD"
    val ubicacion: String? = null,
    val aviso: String? = null,         // ej.: "30 min antes"
    val actualizadoEn: Long = 0L,
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)

/** Prioridades que se ofrecen en el formulario, de mayor a menor. */
val PRIORIDADES: List<Prioridad> get() = listOf(Prioridad.Alta, Prioridad.Media, Prioridad.Baja)

/**
 * Datos que completa la persona en el formulario (crear o editar).
 * No incluye el id ni los datos de sincronización: los asigna el repositorio.
 */
data class DatosRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val fechaLimite: String?
)

// ---------------------------------------------------------------------------
// FUNCIONES DE EXTENSIÓN. Agregan comportamiento al modelo sin modificar la
// clase y sin mutar el objeto original.
// ---------------------------------------------------------------------------

/** true si el recordatorio todavía no se completó. */
fun Recordatorio.estaPendiente(): Boolean = estado !is EstadoRecordatorio.Completado

/** Devuelve una NUEVA versión completada; el original no cambia (copy). */
fun Recordatorio.marcarCompletado(fecha: String): Recordatorio =
    copy(estado = EstadoRecordatorio.Completado(fecha))

/** true si el cambio local todavía no fue confirmado por el servidor. */
fun Recordatorio.tieneCambiosSinConfirmar(): Boolean = estadoSync != EstadoSync.SINCRONIZADO

/** Texto legible del estado, resuelto con un `when` exhaustivo (sin else). */
fun EstadoRecordatorio.etiqueta(): String = when (this) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    EstadoRecordatorio.EnCurso -> "En curso"
    is EstadoRecordatorio.Completado -> "Completado"
}

/** Texto legible del estado de sincronización. */
fun EstadoSync.etiqueta(): String = when (this) {
    EstadoSync.SINCRONIZADO -> "Sincronizado"
    EstadoSync.PENDIENTE -> "Pendiente de sincronizar"
    EstadoSync.PENDIENTE_ELIMINAR -> "Eliminación pendiente"
    EstadoSync.ERROR -> "Error de sincronización"
}
