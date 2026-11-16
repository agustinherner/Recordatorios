package com.example.recordatorios.data.remoto

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configuración de OkHttp y Retrofit (Módulo 3 · U2 · Tema 1, "Configuración del cliente").
 */
object ClienteRemoto {

    /**
     * JSON tolerante:
     * - ignoreUnknownKeys: si el servicio agrega campos nuevos, la app no falla.
     * - coerceInputValues: un null en un campo no anulable usa su valor por defecto.
     * - explicitNulls = false: no envía los campos null.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * @param urlBase URL configurable (BuildConfig.API_BASE_URL). Debe terminar en "/".
     * @param interceptorSimulado si no es null, responde en lugar del servicio real.
     */
    fun crearApi(urlBase: String, interceptorSimulado: Interceptor?): RecordatorioApi {
        val okHttp = OkHttpClient.Builder()
            // Tiempos máximos: evitan que una solicitud quede pendiente para siempre
            // si el servidor no responde o la conexión se corta.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .apply { interceptorSimulado?.let { addInterceptor(it) } }
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(okHttp)
            // Conversor JSON con kotlinx.serialization (el módulo muestra Moshi como ejemplo;
            // se eligió este porque el proyecto ya usa el plugin de serialización).
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RecordatorioApi::class.java)
    }
}
