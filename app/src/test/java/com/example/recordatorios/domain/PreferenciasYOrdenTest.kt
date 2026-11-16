package com.example.recordatorios.domain

import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.ordenadosPor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenciasYOrdenTest {

    @Test
    fun valorGuardadoInvalido_devuelveNull_yseUsaElPredeterminado() {
        assertNull(enumDesdeTexto<TemaApp>("VIOLETA"))
        val tema = enumDesdeTexto<TemaApp>("VIOLETA") ?: PreferenciasUsuario().tema
        assertEquals(TemaApp.SISTEMA, tema)
    }

    @Test
    fun valorGuardadoValido_seRecupera() {
        assertEquals(OrdenListado.FECHA_LIMITE, enumDesdeTexto<OrdenListado>("FECHA_LIMITE"))
    }

    @Test
    fun ordenAlfabetico() {
        val titulos = DatosSimulados.recordatorios.ordenadosPor(OrdenListado.ALFABETICO).map { it.titulo.lowercase() }
        assertEquals(titulos.sorted(), titulos)
    }

    @Test
    fun ordenPorFecha_dejaLosSinFechaAlFinal() {
        val ordenados = DatosSimulados.recordatorios.ordenadosPor(OrdenListado.FECHA_LIMITE)
        val primeroSinFecha = ordenados.indexOfFirst { it.fechaLimite == null }
        assertTrue(ordenados.drop(primeroSinFecha).all { it.fechaLimite == null })
        val fechas = ordenados.mapNotNull { it.fechaLimite }
        assertEquals(fechas.sorted(), fechas)
    }

    @Test
    fun codigoDeEstadoSyncDesconocido_esPendiente() {
        assertEquals(EstadoSync.PENDIENTE, EstadoSync.desdeCodigo("RARO"))
    }
}
