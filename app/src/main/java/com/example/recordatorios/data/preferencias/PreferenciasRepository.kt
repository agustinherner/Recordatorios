package com.example.recordatorios.data.preferencias

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.recordatorios.domain.FiltroPrioridad
import com.example.recordatorios.domain.OrdenListado
import com.example.recordatorios.domain.PreferenciasUsuario
import com.example.recordatorios.domain.TemaApp
import com.example.recordatorios.domain.enumDesdeTexto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * ÚNICA instancia de Preferences DataStore de la app (Módulo 3 · U1 · Tema 1).
 *
 * El delegado preferencesDataStore se declara UNA vez, en el nivel superior
 * del archivo. Si se crearan varias instancias sobre el mismo archivo, DataStore
 * lanzaría un error y distintas partes de la app podrían pisarse los cambios.
 *
 * Qué se guarda acá: SOLO configuraciones simples (tema, mostrar completados,
 * orden, filtro de prioridad). Los recordatorios van en Room. Nunca se guardan
 * contraseñas, credenciales ni tokens.
 */
val Context.preferenciasDataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias_usuario")

/** Claves TIPADAS. Su nombre interno ("tema", ...) debe mantenerse estable entre versiones. */
private object ClavesPreferencias {
    val TEMA = stringPreferencesKey("tema")
    val MOSTRAR_COMPLETADOS = booleanPreferencesKey("mostrar_completados")
    val ORDEN = stringPreferencesKey("orden_listado")
    val FILTRO_PRIORIDAD = stringPreferencesKey("filtro_prioridad")
}

/**
 * Repositorio de preferencias: transforma los pares clave-valor de DataStore
 * en [PreferenciasUsuario] y expone las operaciones de modificación.
 */
class PreferenciasRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Flujo con las preferencias actuales. Emite de nuevo cada vez que cambia una.
     *
     * - catch: si la LECTURA falla con IOException (error recuperable), se emiten
     *   preferencias vacías y se usan los valores predeterminados. Otros errores
     *   se relanzan: podrían ser errores de programación que conviene ver.
     * - map: convierte los pares en el modelo propio. Un valor guardado que no
     *   corresponde a ninguna opción (dato inválido) usa el predeterminado.
     */
    val preferencias: Flow<PreferenciasUsuario> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.e(TAG, "No se pudieron leer las preferencias; se usan los valores predeterminados", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { valores -> valores.aPreferenciasUsuario() }
        .distinctUntilChanged()

    // -------------------------------------------------------------------------
    // Modificaciones: edit es suspend, atómico (lee, modifica y escribe) y solo
    // cambia las claves indicadas. Se llaman desde viewModelScope, nunca desde
    // el hilo principal de forma bloqueante.
    // -------------------------------------------------------------------------

    suspend fun cambiarTema(tema: TemaApp) {
        dataStore.edit { it[ClavesPreferencias.TEMA] = tema.name }
    }

    suspend fun cambiarMostrarCompletados(mostrar: Boolean) {
        dataStore.edit { it[ClavesPreferencias.MOSTRAR_COMPLETADOS] = mostrar }
    }

    suspend fun cambiarOrden(orden: OrdenListado) {
        dataStore.edit { it[ClavesPreferencias.ORDEN] = orden.name }
    }

    suspend fun cambiarFiltroPrioridad(filtro: FiltroPrioridad) {
        dataStore.edit { it[ClavesPreferencias.FILTRO_PRIORIDAD] = filtro.name }
    }

    /** Vuelve todas las preferencias a sus valores predeterminados. */
    suspend fun restablecer() {
        dataStore.edit { it.clear() }
    }

    /**
     * SOLO PARA PRUEBAS (consigna 1): guarda un valor que no corresponde a
     * ninguna opción, como podría dejarlo una versión vieja o un archivo dañado,
     * para comprobar que la app usa el valor predeterminado sin cerrarse.
     */
    suspend fun escribirValorInvalidoDePrueba() {
        dataStore.edit {
            it[ClavesPreferencias.TEMA] = "VIOLETA"
            it[ClavesPreferencias.ORDEN] = "POR_COLOR"
        }
    }

    private fun Preferences.aPreferenciasUsuario(): PreferenciasUsuario {
        val predeterminadas = PreferenciasUsuario()
        return PreferenciasUsuario(
            tema = leerEnum(ClavesPreferencias.TEMA, predeterminadas.tema),
            mostrarCompletados = this[ClavesPreferencias.MOSTRAR_COMPLETADOS] ?: predeterminadas.mostrarCompletados,
            orden = leerEnum(ClavesPreferencias.ORDEN, predeterminadas.orden),
            filtroPrioridad = leerEnum(ClavesPreferencias.FILTRO_PRIORIDAD, predeterminadas.filtroPrioridad)
        )
    }

    /**
     * Lee una opción guardada como texto. Tres casos:
     * - no hay valor guardado → predeterminado (primer uso);
     * - valor válido → esa opción;
     * - valor desconocido → predeterminado y advertencia en Logcat.
     */
    private inline fun <reified T : Enum<T>> Preferences.leerEnum(
        clave: Preferences.Key<String>,
        predeterminado: T
    ): T {
        val guardado = this[clave] ?: return predeterminado
        return enumDesdeTexto<T>(guardado) ?: run {
            Log.w(TAG, "Valor inválido \"$guardado\" en la preferencia ${clave.name}; se usa ${predeterminado.name}")
            predeterminado
        }
    }

    private companion object {
        const val TAG = "Preferencias"
    }
}
