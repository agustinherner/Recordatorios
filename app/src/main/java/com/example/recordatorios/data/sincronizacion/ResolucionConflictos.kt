package com.example.recordatorios.data.sincronizacion

import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.domain.EstadoSync

/**
 * Qué hacer con un registro que llegó del servidor.
 */
sealed interface DecisionFusion {
    /** Guardar la versión remota en Room (queda SINCRONIZADO). */
    data class GuardarRemoto(val recordatorio: RecordatorioEntity) : DecisionFusion

    /** Conservar la versión local (se enviará al servidor en esta misma sincronización). */
    data object ConservarLocal : DecisionFusion

    /** Las dos versiones ya son iguales: no hay nada que escribir. */
    data object SinCambios : DecisionFusion
}

/**
 * RESOLUCIÓN DE CONFLICTOS — last-write-wins (Módulo 3 · U2 · Tema 4, Tabla 5).
 *
 * Hay conflicto cuando el mismo recordatorio cambió en el dispositivo (todavía
 * no enviado) y también en el servidor. Se conserva la versión con el
 * actualizadoEn más reciente.
 *
 * | Situación                                   | Decisión          |
 * |---------------------------------------------|-------------------|
 * | Solo existe la versión remota               | GuardarRemoto     |
 * | Local SINCRONIZADO (sin cambios propios)    | GuardarRemoto (si difiere) |
 * | Local con cambios, la remota es más reciente| GuardarRemoto     |
 * | Local con cambios, la local es más reciente | ConservarLocal    |
 *
 * Una eliminación local pendiente también participa: si el otro dispositivo
 * modificó el recordatorio DESPUÉS de que se eliminó acá, gana la edición remota.
 *
 * Límite conocido: compara el registro COMPLETO, no campo por campo. Si en un
 * dispositivo cambió la prioridad y en otro la descripción, uno de los dos
 * cambios se pierde. También depende de que los relojes de los dispositivos
 * estén razonablemente sincronizados.
 *
 * Es una función PURA (sin Room ni red): se prueba con tests unitarios.
 */
fun decidirFusion(local: RecordatorioEntity?, remoto: RecordatorioEntity): DecisionFusion = when {
    local == null -> DecisionFusion.GuardarRemoto(remoto)

    local.estadoSync == EstadoSync.SINCRONIZADO ->
        if (local == remoto) DecisionFusion.SinCambios else DecisionFusion.GuardarRemoto(remoto)

    // Local con cambios sin enviar (PENDIENTE, PENDIENTE_ELIMINAR o ERROR): conflicto.
    remoto.actualizadoEn > local.actualizadoEn -> DecisionFusion.GuardarRemoto(remoto)

    else -> DecisionFusion.ConservarLocal
}

/**
 * Registros locales que ya estaban sincronizados y que el servidor ya no
 * tiene: fueron eliminados desde otro dispositivo, así que se eliminan acá.
 * Los que tienen cambios sin enviar NO se tocan (se volverán a crear al subirlos).
 */
fun idsEliminadosEnServidor(locales: Collection<RecordatorioEntity>, idsRemotos: Set<String>): List<String> =
    locales
        .filter { it.estadoSync == EstadoSync.SINCRONIZADO && it.id !in idsRemotos }
        .map { it.id }
