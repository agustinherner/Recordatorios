package com.example.recordatorios.data.remoto

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Contrato del servicio remoto declarado con Retrofit
 * (Módulo 3 · U2 · Tema 1, "Declaración de los endpoints").
 *
 * Cada función es una operación HTTP. Son `suspend` para ejecutarse desde
 * coroutines sin bloquear el hilo principal. Devuelven Response<T> para poder
 * leer el CÓDIGO HTTP y no solo el cuerpo.
 *
 * Las rutas son relativas a la URL base (BuildConfig.API_BASE_URL).
 */
interface RecordatorioApi {

    /** Todos los recordatorios del servidor. */
    @GET("recordatorios")
    suspend fun listar(): Response<List<RecordatorioDto>>

    /** Un recordatorio por id. */
    @GET("recordatorios/{id}")
    suspend fun obtener(@Path("id") id: String): Response<RecordatorioDto>

    /**
     * Crea o REEMPLAZA el recurso con ese id. PUT es idempotente: enviarlo dos
     * veces deja el mismo resultado que enviarlo una. Es la base de la
     * estrategia para evitar duplicados al reintentar.
     */
    @PUT("recordatorios/{id}")
    suspend fun guardar(@Path("id") id: String, @Body recordatorio: RecordatorioDto): Response<RecordatorioDto>

    /**
     * Crea un recurso nuevo. Solo se usa si el servicio no permite crear con
     * PUT (json-server responde 404 al PUT de un id inexistente). Como el id lo
     * genera la app, un POST repetido no crea un segundo registro: el servidor
     * lo rechaza por id duplicado.
     */
    @POST("recordatorios")
    suspend fun crear(@Body recordatorio: RecordatorioDto): Response<RecordatorioDto>

    /** Elimina el recurso. */
    @DELETE("recordatorios/{id}")
    suspend fun eliminar(@Path("id") id: String): Response<Unit>
}
