package com.example.recordatorios.data.local

import androidx.room.TypeConverter
import com.example.recordatorios.domain.EstadoSync

/**
 * Conversores de tipos (Módulo 3 · U1 · Tema 2, "Conversión de tipos").
 *
 * SQLite no conoce los enum de Kotlin. El conversor indica cómo pasar
 * EstadoSync a texto y de vuelta. Se usa el [EstadoSync.codigo] (estable) y
 * no `name`/`valueOf`: si se renombrara la constante del enum, los valores
 * guardados seguirían siendo reconocidos; y un código desconocido no rompe
 * la lectura, se interpreta como PENDIENTE (se reenvía, sin riesgo de
 * duplicar porque el envío es idempotente).
 */
class Conversores {

    @TypeConverter
    fun estadoSyncATexto(estado: EstadoSync): String = estado.codigo

    @TypeConverter
    fun textoAEstadoSync(valor: String): EstadoSync = EstadoSync.desdeCodigo(valor)
}
