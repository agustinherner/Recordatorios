package com.example.recordatorios.data.remoto

/**
 * Resultado CONTROLADO de una llamada al servicio
 * (Módulo 3 · U2 · Tema 1, "Interpretación de las respuestas HTTP", Tabla 3).
 *
 * Es un sealed interface: en lugar de lanzar excepciones o devolver null,
 * cada situación es una variante concreta y quien la recibe está obligado a
 * contemplarlas todas con un `when` exhaustivo. Así todas las partes de la app
 * interpretan los errores de la MISMA manera (problema detectado en la consigna).
 */
sealed interface ResultadoRemoto<out T> {

    /** 2xx con datos válidos. */
    data class Exito<T>(val datos: T) : ResultadoRemoto<T>

    /** 4xx: la solicitud no pudo atenderse (datos inválidos, recurso inexistente, autorización). No se reintenta automáticamente. */
    data class SolicitudInvalida(val codigo: Int) : ResultadoRemoto<Nothing>

    /** 5xx: falló el servidor. Es transitorio: admite un reintento posterior. */
    data class ErrorServidor(val codigo: Int) : ResultadoRemoto<Nothing>

    /** No hubo respuesta en el tiempo previsto (SocketTimeoutException). Admite reintento. */
    data object Timeout : ResultadoRemoto<Nothing>

    /** No se pudo establecer la comunicación (IOException). Se sigue con los datos locales. */
    data object SinConexion : ResultadoRemoto<Nothing>

    /** 2xx sin el cuerpo esperado, o JSON que no respeta el contrato. */
    data class DatosInvalidos(val causa: Throwable) : ResultadoRemoto<Nothing>
}

/** true si el fallo es transitorio y tiene sentido reintentar más tarde. */
fun ResultadoRemoto<*>.esReintentable(): Boolean = when (this) {
    is ResultadoRemoto.ErrorServidor, ResultadoRemoto.Timeout, ResultadoRemoto.SinConexion -> true
    is ResultadoRemoto.Exito, is ResultadoRemoto.SolicitudInvalida, is ResultadoRemoto.DatosInvalidos -> false
}

/** Descripción breve para registrar en Logcat (sin datos personales). */
fun ResultadoRemoto<*>.descripcion(): String = when (this) {
    is ResultadoRemoto.Exito -> "éxito"
    is ResultadoRemoto.SolicitudInvalida -> "solicitud inválida (HTTP $codigo)"
    is ResultadoRemoto.ErrorServidor -> "error del servidor (HTTP $codigo)"
    ResultadoRemoto.Timeout -> "timeout"
    ResultadoRemoto.SinConexion -> "sin conexión"
    is ResultadoRemoto.DatosInvalidos -> "datos inválidos (${causa::class.simpleName})"
}
