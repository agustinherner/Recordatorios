package com.example.recordatorios.data

import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.sincronizacion.DecisionFusion
import com.example.recordatorios.data.sincronizacion.decidirFusion
import com.example.recordatorios.data.sincronizacion.idsEliminadosEnServidor
import com.example.recordatorios.domain.EstadoSync
import org.junit.Assert.assertEquals
import org.junit.Test

/** Casos de la Tabla 5 del Módulo 3 (last-write-wins). */
class ResolucionConflictosTest {

    private fun entidad(
        id: String = "r1",
        titulo: String = "Título",
        actualizadoEn: Long,
        estadoSync: EstadoSync
    ) = RecordatorioEntity(
        id = id, titulo = titulo, descripcion = null, categoria = "Trabajo", prioridad = "MEDIA",
        estado = "PENDIENTE", fechaCompletado = null, fechaLimite = null, ubicacion = null, aviso = null,
        actualizadoEn = actualizadoEn, estadoSync = estadoSync
    )

    @Test
    fun soloExisteLaRemota_seGuarda() {
        val remoto = entidad(actualizadoEn = 100, estadoSync = EstadoSync.SINCRONIZADO)
        assertEquals(DecisionFusion.GuardarRemoto(remoto), decidirFusion(null, remoto))
    }

    @Test
    fun localSincronizado_yRemotaDistinta_seGuardaLaRemota() {
        val local = entidad(titulo = "Viejo", actualizadoEn = 100, estadoSync = EstadoSync.SINCRONIZADO)
        val remoto = entidad(titulo = "Nuevo", actualizadoEn = 200, estadoSync = EstadoSync.SINCRONIZADO)
        assertEquals(DecisionFusion.GuardarRemoto(remoto), decidirFusion(local, remoto))
    }

    @Test
    fun versionesIguales_noSeEscribeNada() {
        val local = entidad(actualizadoEn = 100, estadoSync = EstadoSync.SINCRONIZADO)
        assertEquals(DecisionFusion.SinCambios, decidirFusion(local, local.copy()))
    }

    @Test
    fun conflicto_localMasReciente_seConservaLaLocal() {
        val local = entidad(titulo = "Editado offline", actualizadoEn = 300, estadoSync = EstadoSync.PENDIENTE)
        val remoto = entidad(titulo = "Editado en otro dispositivo", actualizadoEn = 200, estadoSync = EstadoSync.SINCRONIZADO)
        assertEquals(DecisionFusion.ConservarLocal, decidirFusion(local, remoto))
    }

    @Test
    fun conflicto_remotaMasReciente_seGuardaLaRemota() {
        val local = entidad(titulo = "Editado offline", actualizadoEn = 200, estadoSync = EstadoSync.PENDIENTE)
        val remoto = entidad(titulo = "Editado en otro dispositivo", actualizadoEn = 300, estadoSync = EstadoSync.SINCRONIZADO)
        assertEquals(DecisionFusion.GuardarRemoto(remoto), decidirFusion(local, remoto))
    }

    @Test
    fun eliminacionPendiente_masReciente_seConserva() {
        val local = entidad(actualizadoEn = 300, estadoSync = EstadoSync.PENDIENTE_ELIMINAR)
        val remoto = entidad(actualizadoEn = 100, estadoSync = EstadoSync.SINCRONIZADO)
        assertEquals(DecisionFusion.ConservarLocal, decidirFusion(local, remoto))
    }

    @Test
    fun eliminadosEnOtroDispositivo_soloSiEstabanSincronizados() {
        val locales = listOf(
            entidad(id = "sincronizado-borrado", actualizadoEn = 1, estadoSync = EstadoSync.SINCRONIZADO),
            entidad(id = "nuevo-sin-enviar", actualizadoEn = 2, estadoSync = EstadoSync.PENDIENTE),
            entidad(id = "sigue-en-servidor", actualizadoEn = 3, estadoSync = EstadoSync.SINCRONIZADO)
        )
        val idsRemotos = setOf("sigue-en-servidor")
        assertEquals(listOf("sincronizado-borrado"), idsEliminadosEnServidor(locales, idsRemotos))
    }
}
