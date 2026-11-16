package com.example.recordatorios.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.recordatorios.domain.EstadoSync

/**
 * Fila de la tabla "recordatorios" (Módulo 3 · U1 · Tema 2, "Definición de una entidad").
 *
 * Responde al ESQUEMA DE ROOM, no a la interfaz ni al servicio: por eso
 * guarda prioridad y estado como códigos de texto estables y no como las
 * sealed classes del dominio. La conversión la hacen los mapeos (Mapeos.kt).
 *
 * - [id]: UUID generado en el dispositivo. Es clave primaria y TAMBIÉN el
 *   identificador en el servidor, así que el mismo recordatorio nunca puede
 *   quedar dos veces en la tabla (la clave primaria lo impide) y un reintento
 *   de sincronización actualiza el mismo recurso remoto.
 * - [actualizadoEn]: última modificación; resuelve conflictos (last-write-wins).
 * - [estadoSync]: si el registro está sincronizado, pendiente o con error.
 *   Se guarda con un TypeConverter (Conversores.kt).
 *
 * Los índices aceleran las consultas que filtran por estado de sincronización
 * y por prioridad.
 */
@Entity(
    tableName = "recordatorios",
    indices = [
        Index(value = ["estado_sync"]),
        Index(value = ["prioridad"])
    ]
)
data class RecordatorioEntity(
    @PrimaryKey
    val id: String,

    val titulo: String,

    val descripcion: String?,

    val categoria: String,

    /** Código de Prioridad: "ALTA", "MEDIA" o "BAJA". */
    val prioridad: String,

    /** Código de EstadoRecordatorio: "PENDIENTE", "EN_CURSO" o "COMPLETADO". */
    val estado: String,

    @ColumnInfo(name = "fecha_completado")
    val fechaCompletado: String?,

    /** "AAAA-MM-DD" o null. */
    @ColumnInfo(name = "fecha_limite")
    val fechaLimite: String?,

    val ubicacion: String?,

    val aviso: String?,

    @ColumnInfo(name = "actualizado_en")
    val actualizadoEn: Long,

    @ColumnInfo(name = "estado_sync")
    val estadoSync: EstadoSync
)
