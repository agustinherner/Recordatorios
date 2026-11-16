package com.example.recordatorios.data.remoto

import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fuente de datos REMOTA (Módulo 3 · U2 · Tema 1).
 *
 * Es la única clase que habla con Retrofit. Traduce las respuestas y
 * excepciones técnicas a un [ResultadoRemoto]. Las pantallas nunca ven un
 * código HTTP ni una excepción de red: esa interpretación vive acá, en la
 * capa de datos.
 */
class FuenteRemotaRecordatorios(private val api: RecordatorioApi) {

    suspend fun listar(): ResultadoRemoto<List<RecordatorioDto>> = ejecutar { api.listar() }

    /**
     * Envía un recordatorio (crear o actualizar) de forma IDEMPOTENTE.
     * 1) PUT recordatorios/{id}: crea o reemplaza ese id.
     * 2) Si el servicio responde 404 (no admite crear con PUT, como json-server),
     *    se crea con POST conservando el MISMO id generado por la app.
     * En ningún caso se genera un id nuevo, así que repetir el envío no duplica.
     */
    suspend fun guardar(dto: RecordatorioDto): ResultadoRemoto<RecordatorioDto> {
        val resultado = ejecutar { api.guardar(dto.id, dto) }
        return if (resultado is ResultadoRemoto.SolicitudInvalida && resultado.codigo == HTTP_NO_ENCONTRADO) {
            ejecutar { api.crear(dto) }
        } else {
            resultado
        }
    }

    /**
     * Elimina en el servidor. Un 404 se considera ÉXITO: el recurso ya no
     * existe (por ejemplo, porque un intento anterior se completó pero la
     * respuesta se perdió). Eliminar también es idempotente.
     */
    suspend fun eliminar(id: String): ResultadoRemoto<Unit> =
        // cuerpoSiVacio = Unit: un 200/204 sin cuerpo es una eliminación exitosa.
        when (val resultado = ejecutar(cuerpoSiVacio = Unit) { api.eliminar(id) }) {
            is ResultadoRemoto.SolicitudInvalida ->
                if (resultado.codigo == HTTP_NO_ENCONTRADO) ResultadoRemoto.Exito(Unit) else resultado
            else -> resultado
        }

    /**
     * Ejecuta una llamada y la clasifica. Orden de los catch:
     * - CancellationException se relanza: la cancelación de una coroutine no es un error de red.
     * - SocketTimeoutException ANTES que IOException, porque es una subclase:
     *   si fuera al revés, un timeout se clasificaría como "sin conexión".
     *
     * [cuerpoSiVacio]: valor a usar cuando un 2xx llega sin cuerpo y eso es
     * válido (DELETE con 204). Si es null, un 2xx sin cuerpo es un dato inválido.
     */
    private suspend fun <T : Any> ejecutar(
        cuerpoSiVacio: T? = null,
        llamada: suspend () -> Response<T>
    ): ResultadoRemoto<T> =
        try {
            val respuesta = llamada()
            val codigo = respuesta.code()
            when {
                respuesta.isSuccessful -> {
                    // Un 2xx no garantiza que el cuerpo sea válido: también se controla.
                    val cuerpo = respuesta.body() ?: cuerpoSiVacio
                    if (cuerpo != null) ResultadoRemoto.Exito(cuerpo)
                    else ResultadoRemoto.DatosInvalidos(IllegalStateException("La respuesta $codigo no contiene datos"))
                }
                codigo in 400..499 -> ResultadoRemoto.SolicitudInvalida(codigo)
                else -> ResultadoRemoto.ErrorServidor(codigo)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            ResultadoRemoto.Timeout
        } catch (e: IOException) {
            ResultadoRemoto.SinConexion
        } catch (e: SerializationException) {
            ResultadoRemoto.DatosInvalidos(e)
        } catch (e: IllegalArgumentException) {
            // kotlinx.serialization también informa JSON incompatible con IllegalArgumentException.
            ResultadoRemoto.DatosInvalidos(e)
        }

    private companion object {
        const val HTTP_NO_ENCONTRADO = 404
    }
}
