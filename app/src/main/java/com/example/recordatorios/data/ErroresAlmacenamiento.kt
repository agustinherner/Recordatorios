package com.example.recordatorios.data

import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import java.io.IOException

/**
 * Errores de almacenamiento local (Módulo 3 · U1 · Tema 4, Tabla 2).
 *
 * Mismo patrón que ResultadoRemoto y ResultadoSincronizacion: un sealed
 * interface con variantes concretas, en lugar de dejar pasar la excepción
 * técnica hasta la interfaz.
 */
sealed interface ErrorAlmacenamiento {
    data object SinEspacio : ErrorAlmacenamiento
    data object DatosCorruptos : ErrorAlmacenamiento
    data object NoEncontrado : ErrorAlmacenamiento
    data object AccesoFallido : ErrorAlmacenamiento
    data class Desconocido(val causa: Throwable) : ErrorAlmacenamiento
}

/** Excepción que transporta el error ya clasificado dentro de un Result.failure. */
class ErrorAlmacenamientoException(
    val error: ErrorAlmacenamiento,
    causa: Throwable? = null
) : Exception("Error de almacenamiento: $error", causa)

/**
 * Clasifica la excepción técnica. Las subclases van ANTES que SQLiteException
 * (SQLiteFullException y SQLiteDatabaseCorruptException heredan de ella).
 */
fun Throwable.aErrorAlmacenamiento(): ErrorAlmacenamiento = when (this) {
    is ErrorAlmacenamientoException -> error
    is SQLiteFullException -> ErrorAlmacenamiento.SinEspacio
    is SQLiteDatabaseCorruptException -> ErrorAlmacenamiento.DatosCorruptos
    is NoSuchElementException -> ErrorAlmacenamiento.NoEncontrado
    is SQLiteException, is IOException -> ErrorAlmacenamiento.AccesoFallido
    else -> ErrorAlmacenamiento.Desconocido(this)
}

/**
 * Mensaje ORIENTADO A LA PERSONA (nunca el texto crudo de la excepción).
 * El detalle técnico se registra en Logcat para el equipo.
 */
fun ErrorAlmacenamiento.mensajeParaUsuario(): String = when (this) {
    ErrorAlmacenamiento.SinEspacio ->
        "No hay espacio suficiente para guardar el recordatorio. Liberá espacio y volvé a intentarlo."
    ErrorAlmacenamiento.DatosCorruptos ->
        "Los datos guardados están dañados. No se realizaron cambios."
    ErrorAlmacenamiento.NoEncontrado ->
        "El recordatorio ya no existe."
    ErrorAlmacenamiento.AccesoFallido ->
        "No se pudo acceder al almacenamiento del dispositivo. Intentá nuevamente."
    is ErrorAlmacenamiento.Desconocido ->
        "No se pudo guardar el cambio. Intentá nuevamente."
}

/** Mensaje para cualquier falla de una operación local (validación o almacenamiento). */
fun Throwable.mensajeOperacionLocal(): String = when (this) {
    is IllegalArgumentException -> message ?: "Los datos ingresados no son válidos."
    else -> aErrorAlmacenamiento().mensajeParaUsuario()
}
