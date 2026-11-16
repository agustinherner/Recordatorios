package com.example.recordatorios.data.remoto

import kotlinx.serialization.Serializable

/**
 * DTO (Data Transfer Object): la forma en que el SERVICIO representa un
 * recordatorio (Módulo 3 · U2 · Tema 1, "Definición del DTO remoto").
 *
 * Refleja el contrato remoto, no la base ni la interfaz: por ejemplo, la
 * prioridad llega como texto libre ("ALTA") y podría venir con un valor
 * desconocido. Los mapeos (Mapeos.kt) validan y convierten el DTO en
 * RecordatorioEntity ANTES de guardarlo en Room. Ningún DTO llega a Compose.
 *
 * @Serializable: kotlinx.serialization genera en compilación el código para
 * convertir a/desde JSON (el mismo plugin que ya usan las rutas tipadas).
 */
@Serializable
data class RecordatorioDto(
    val id: String,
    val titulo: String,
    val descripcion: String? = null,
    val categoria: String,
    val prioridad: String,
    val estado: String,
    val fechaCompletado: String? = null,
    val fechaLimite: String? = null,
    val ubicacion: String? = null,
    val aviso: String? = null,
    val actualizadoEn: Long
)
