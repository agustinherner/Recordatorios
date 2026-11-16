package com.example.recordatorios.data

import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas de los mapeos entre capas (DTO → Entity → Dominio).
 * Son pruebas locales: corren en la PC, sin emulador.
 */
class MapeosTest {

    private fun dto(
        id: String = "abc-123",
        titulo: String = "Pagar la luz",
        prioridad: String = "ALTA",
        estado: String = "PENDIENTE",
        fechaCompletado: String? = null
    ) = RecordatorioDto(
        id = id, titulo = titulo, categoria = "Hogar", prioridad = prioridad,
        estado = estado, fechaCompletado = fechaCompletado, actualizadoEn = 1_000L
    )

    @Test
    fun dtoValido_seConvierteEnEntidadSincronizada() {
        val entidad = dto().aEntityOrNull()
        assertNotNull(entidad)
        assertEquals("abc-123", entidad!!.id)
        assertEquals(EstadoSync.SINCRONIZADO, entidad.estadoSync)
        assertEquals("ALTA", entidad.prioridad)
    }

    @Test
    fun prioridadDesconocida_usaMedia() {
        val entidad = dto(prioridad = "URGENTISIMA").aEntityOrNull()!!
        assertEquals(Prioridad.Media.codigo, entidad.prioridad)
    }

    @Test
    fun estadoDesconocido_usaPendiente() {
        val dominio = dto(estado = "ARCHIVADO").aEntityOrNull()!!.aDominio()
        assertEquals(EstadoRecordatorio.Pendiente, dominio.estado)
    }

    @Test
    fun tituloVacio_seDescarta() {
        assertNull(dto(titulo = "   ").aEntityOrNull())
    }

    @Test
    fun idVacio_seDescarta() {
        assertNull(dto(id = "").aEntityOrNull())
    }

    @Test
    fun completadoConservaLaFecha_enIdaYVuelta() {
        val original = dto(estado = "COMPLETADO", fechaCompletado = "2026-11-01")
        val dominio = original.aEntityOrNull()!!.aDominio()
        assertEquals(EstadoRecordatorio.Completado("2026-11-01"), dominio.estado)
        // Dominio → Entity → DTO devuelve los mismos datos que llegaron.
        assertEquals(original, dominio.aEntity().aDto())
    }

    @Test
    fun dominioAEntidad_yVuelta_esIgual() {
        val recordatorio = DatosSimulados.recordatorios.first().copy(estadoSync = EstadoSync.PENDIENTE)
        assertEquals(recordatorio, recordatorio.aEntity().aDominio())
    }
}
