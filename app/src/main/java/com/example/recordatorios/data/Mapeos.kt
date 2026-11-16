package com.example.recordatorios.data

import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio

// ---------------------------------------------------------------------------
// MAPEOS EXPLÍCITOS ENTRE CAPAS (Módulo 3 · U1 · Tema 3, "Modelos y contrato
// del repositorio").
//
//            aEntity()               aDominio()
//   DTO  ───────────────►  Entity  ───────────────►  Recordatorio (dominio)
//   (servicio)   ◄──────── (Room)   ◄───────────────  (ViewModel / Compose)
//                 aDto()              aEntity(estadoSync)
//
// Gracias a esta separación, si cambia el formato del servicio o el esquema
// de Room, solo se tocan estas funciones: las pantallas no se enteran.
// ---------------------------------------------------------------------------

/** Valores por defecto ante datos remotos desconocidos. */
private val PRIORIDAD_POR_DEFECTO: Prioridad = Prioridad.Media
private const val CATEGORIA_POR_DEFECTO = "Personal"

// --- Entity ↔ Dominio ------------------------------------------------------

/** Fila de Room → modelo que usa la app. */
fun RecordatorioEntity.aDominio(): Recordatorio = Recordatorio(
    id = id,
    titulo = titulo,
    categoria = Categoria(categoria),
    prioridad = Prioridad.desdeCodigo(prioridad) ?: PRIORIDAD_POR_DEFECTO,
    estado = estadoDesdeCodigo(estado, fechaCompletado),
    descripcion = descripcion,
    fechaLimite = fechaLimite,
    ubicacion = ubicacion,
    aviso = aviso,
    actualizadoEn = actualizadoEn,
    estadoSync = estadoSync
)

/** Modelo de la app → fila de Room, con el estado de sincronización indicado. */
fun Recordatorio.aEntity(estadoSync: EstadoSync = this.estadoSync): RecordatorioEntity = RecordatorioEntity(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.nombre,
    prioridad = prioridad.codigo,
    estado = estado.codigo,
    fechaCompletado = (estado as? EstadoRecordatorio.Completado)?.fechaCompletado,
    fechaLimite = fechaLimite,
    ubicacion = ubicacion,
    aviso = aviso,
    actualizadoEn = actualizadoEn,
    estadoSync = estadoSync
)

// --- Entity ↔ DTO ------------------------------------------------------------

/** Fila de Room → lo que se envía al servicio. El estado de sincronización NO viaja: es local. */
fun RecordatorioEntity.aDto(): RecordatorioDto = RecordatorioDto(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    prioridad = prioridad,
    estado = estado,
    fechaCompletado = fechaCompletado,
    fechaLimite = fechaLimite,
    ubicacion = ubicacion,
    aviso = aviso,
    actualizadoEn = actualizadoEn
)

/**
 * Lo que llegó del servicio → fila de Room, ya CONFIRMADA (SINCRONIZADO).
 *
 * Es el punto donde se controlan los valores desconocidos ANTES de que lleguen
 * a la base (Módulo 3 · U2 · Tema 1):
 * - id o título vacíos → el registro no es utilizable: se descarta (null).
 * - prioridad desconocida → MEDIA; estado desconocido → PENDIENTE.
 * - categoría vacía → "Personal".
 */
fun RecordatorioDto.aEntityOrNull(): RecordatorioEntity? {
    if (id.isBlank() || titulo.isBlank()) return null
    val prioridadValida = Prioridad.desdeCodigo(prioridad) ?: PRIORIDAD_POR_DEFECTO
    val estadoValido = estadoDesdeCodigo(estado, fechaCompletado)
    return RecordatorioEntity(
        id = id,
        titulo = titulo.trim(),
        descripcion = descripcion?.trim()?.takeIf { it.isNotEmpty() },
        categoria = categoria.trim().ifEmpty { CATEGORIA_POR_DEFECTO },
        prioridad = prioridadValida.codigo,
        estado = estadoValido.codigo,
        fechaCompletado = (estadoValido as? EstadoRecordatorio.Completado)?.fechaCompletado,
        fechaLimite = fechaLimite,
        ubicacion = ubicacion,
        aviso = aviso,
        actualizadoEn = actualizadoEn,
        estadoSync = EstadoSync.SINCRONIZADO
    )
}

// --- Dominio → DTO (contenido inicial del servidor simulado) -----------------

fun Recordatorio.aDto(): RecordatorioDto = aEntity().aDto()

// --- Auxiliares -------------------------------------------------------------

/** Código guardado → EstadoRecordatorio. Un código desconocido se interpreta como Pendiente. */
fun estadoDesdeCodigo(codigo: String?, fechaCompletado: String?): EstadoRecordatorio = when (codigo) {
    EstadoRecordatorio.EnCurso.codigo -> EstadoRecordatorio.EnCurso
    // "COMPLETADO": si faltara la fecha se usa un texto genérico en lugar de fallar.
    "COMPLETADO" -> EstadoRecordatorio.Completado(fechaCompletado ?: "fecha desconocida")
    else -> EstadoRecordatorio.Pendiente
}
