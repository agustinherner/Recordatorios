package com.example.recordatorios.data.remoto

import android.util.Log
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.aDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

/** Fallas que se pueden provocar en la PRÓXIMA solicitud para probar cada caso de la Tabla 3. */
enum class FallaSimulada { ERROR_SERVIDOR, SOLICITUD_INVALIDA, TIMEOUT }

/** Controles del servidor simulado que usa el menú de pruebas de la interfaz. */
interface ControlServidorSimulado {
    fun fallarProximaSolicitud(falla: FallaSimulada)

    /** Modifica un recordatorio "desde otro dispositivo" (para probar conflictos). Devuelve false si no hay datos. */
    fun simularCambioRemoto(): Boolean
}

/**
 * SERVICIO REMOTO SIMULADO (la consigna admite un servicio "previamente
 * disponible o simulado").
 *
 * Es un Interceptor de OkHttp que responde las solicitudes de Retrofit SIN
 * salir a internet: Retrofit, OkHttp, el JSON, los códigos HTTP y las
 * excepciones son reales; lo único simulado es quién contesta. Así la app
 * funciona sin instalar nada, y con API_SIMULADA=false se usa un servicio
 * HTTP real (json-server) con el MISMO código.
 *
 * Para que las pruebas offline sean realistas:
 * - Si el dispositivo no tiene red (modo avión), lanza IOException igual que
 *   OkHttp real → la app lo interpreta como SinConexion.
 * - Agrega una demora para que se vea el estado "sincronizando".
 * - Guarda sus datos en un archivo propio (servidor_simulado.json), separado
 *   de Room, para que se comporte como un servidor que conserva la información.
 */
class ServidorSimulado(
    private val archivo: File,
    private val hayConexion: () -> Boolean,
    private val json: Json,
    private val demoraMs: Long = 800
) : Interceptor, ControlServidorSimulado {

    private val candado = Any()
    private val serializadorLista = ListSerializer(RecordatorioDto.serializer())
    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    /** "Base de datos" del servidor, por id. Se carga del archivo o con los datos de ejemplo. */
    private val datos: MutableMap<String, RecordatorioDto> by lazy { cargar() }

    @Volatile
    private var proximaFalla: FallaSimulada? = null

    override fun fallarProximaSolicitud(falla: FallaSimulada) {
        proximaFalla = falla
    }

    override fun simularCambioRemoto(): Boolean = synchronized(candado) {
        val elegido = datos.values.minByOrNull { it.id } ?: return@synchronized false
        val sufijo = " (editado en otro dispositivo)"
        datos[elegido.id] = elegido.copy(
            titulo = elegido.titulo.removeSuffix(sufijo).take(LARGO_TITULO_BASE) + sufijo,
            actualizadoEn = System.currentTimeMillis()
        )
        persistir()
        true
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitud = chain.request()
        Thread.sleep(demoraMs)   // corre en un hilo de OkHttp, no en el hilo principal

        if (!hayConexion()) throw IOException("Sin conexión (servidor simulado)")

        // La falla se consume: afecta solo a la próxima solicitud.
        val falla = proximaFalla
        proximaFalla = null
        return when (falla) {
            FallaSimulada.TIMEOUT -> throw SocketTimeoutException("Timeout simulado")
            FallaSimulada.ERROR_SERVIDOR -> responder(solicitud, 500, """{"error":"Error interno simulado"}""")
            FallaSimulada.SOLICITUD_INVALIDA -> responder(solicitud, 400, """{"error":"Solicitud inválida simulada"}""")
            null -> procesar(solicitud)
        }
    }

    /** Resuelve la operación según método y ruta: /recordatorios y /recordatorios/{id}. */
    private fun procesar(solicitud: Request): Response = synchronized(candado) {
        val segmentos = solicitud.url.pathSegments.filter { it.isNotEmpty() }.dropWhile { it != RECURSO }
        val id = segmentos.getOrNull(1)

        if (segmentos.isEmpty()) {
            responder(solicitud, 404, """{"error":"Ruta inexistente"}""")
        } else when (solicitud.method) {
            "GET" -> if (id == null) {
                responder(solicitud, 200, json.encodeToString(serializadorLista, datos.values.sortedBy { it.id }))
            } else {
                datos[id]?.let { responder(solicitud, 200, json.encodeToString(RecordatorioDto.serializer(), it)) }
                    ?: responder(solicitud, 404, """{"error":"No existe"}""")
            }

            "PUT" -> {
                val dto = leerCuerpo(solicitud)
                when {
                    id == null || dto == null -> responder(solicitud, 400, """{"error":"Cuerpo inválido"}""")
                    dto.id != id -> responder(solicitud, 400, """{"error":"El id del cuerpo no coincide con la ruta"}""")
                    dto.titulo.isBlank() -> responder(solicitud, 422, """{"error":"El título es obligatorio"}""")
                    else -> {
                        datos[id] = dto          // crea o reemplaza: idempotente
                        persistir()
                        responder(solicitud, 200, json.encodeToString(RecordatorioDto.serializer(), dto))
                    }
                }
            }

            "POST" -> {
                val dto = leerCuerpo(solicitud)
                when {
                    dto == null -> responder(solicitud, 400, """{"error":"Cuerpo inválido"}""")
                    datos.containsKey(dto.id) -> responder(solicitud, 409, """{"error":"Ya existe un recordatorio con ese id"}""")
                    else -> {
                        datos[dto.id] = dto
                        persistir()
                        responder(solicitud, 201, json.encodeToString(RecordatorioDto.serializer(), dto))
                    }
                }
            }

            "DELETE" -> if (id != null && datos.remove(id) != null) {
                persistir()
                responder(solicitud, 200, "{}")
            } else {
                responder(solicitud, 404, """{"error":"No existe"}""")
            }

            else -> responder(solicitud, 405, """{"error":"Método no permitido"}""")
        }
    }

    private fun leerCuerpo(solicitud: Request): RecordatorioDto? = try {
        val buffer = Buffer()
        solicitud.body?.writeTo(buffer)
        json.decodeFromString(RecordatorioDto.serializer(), buffer.readUtf8())
    } catch (e: Exception) {
        null
    }

    private fun responder(solicitud: Request, codigo: Int, cuerpo: String): Response =
        Response.Builder()
            .request(solicitud)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(if (codigo in 200..299) "OK" else "Error")
            .body(cuerpo.toResponseBody(tipoJson))
            .build()

    private fun cargar(): MutableMap<String, RecordatorioDto> {
        val lista = try {
            if (archivo.exists()) json.decodeFromString(serializadorLista, archivo.readText())
            else DatosSimulados.recordatorios.map { it.aDto() }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer el archivo del servidor simulado; se usan los datos de ejemplo", e)
            DatosSimulados.recordatorios.map { it.aDto() }
        }
        return lista.associateBy { it.id }.toMutableMap()
    }

    private fun persistir() {
        try {
            archivo.writeText(json.encodeToString(serializadorLista, datos.values.toList()))
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo guardar el archivo del servidor simulado", e)
        }
    }

    private companion object {
        const val TAG = "ServidorSimulado"
        const val RECURSO = "recordatorios"
        const val LARGO_TITULO_BASE = 25
    }
}
