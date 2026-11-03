package com.example.recordatorios.data

/**
 * Contenedor manual de dependencias.
 *
 * Crea UNA sola instancia del repositorio para toda la app, así el listado,
 * el formulario y el detalle comparten los mismos datos. Al ser un object,
 * sobrevive a la recreación de la Activity (rotación).
 * Expone la instancia con el tipo de las INTERFACES, no de la clase concreta.
 */
object ContenedorDependencias {
    private val repositorioEnMemoria by lazy { RecordatorioRepositoryEnMemoria() }

    val repositorio: RecordatorioRepository get() = repositorioEnMemoria
    val simulacion: ControlSimulacion get() = repositorioEnMemoria
}
