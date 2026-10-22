package com.example.recordatorios.domain

// ---------------------------------------------------------------------------
// REGLAS DE VALIDACIÓN (Módulo 1 · U2 · Tema 3). Los datos inválidos son un
// ERROR ESPERABLE: no se lanzan excepciones ni se cierra la app, se devuelve
// un resultado que la interfaz puede mostrar.
// ---------------------------------------------------------------------------

/** Resultado de validar un recordatorio: válido, o inválido con la lista de motivos. */
sealed class ResultadoValidacion {
    object Valido : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

const val LARGO_MINIMO_TITULO = 4
const val LARGO_MAXIMO_TITULO = 60
const val LARGO_MAXIMO_DESCRIPCION = 200

/** Regla del título. Devuelve null si es válido, o el mensaje de error. */
fun validarTitulo(titulo: String): String? = when {
    titulo.isBlank() -> "El título es obligatorio"
    titulo.trim().length < LARGO_MINIMO_TITULO -> "El título debe tener al menos $LARGO_MINIMO_TITULO caracteres"
    titulo.trim().length > LARGO_MAXIMO_TITULO -> "El título no puede superar $LARGO_MAXIMO_TITULO caracteres"
    else -> null
}

/**
 * Regla de la descripción (dato OPCIONAL).
 * null o vacía es válida; si existe, no puede superar el máximo.
 * Se usa ?. y ?: para tratar el null sin forzarlo con !!.
 */
fun validarDescripcion(descripcion: String?): String? {
    val largo = descripcion?.trim()?.length ?: 0
    return if (largo > LARGO_MAXIMO_DESCRIPCION) {
        "La descripción no puede superar $LARGO_MAXIMO_DESCRIPCION caracteres"
    } else null
}

/**
 * Regla de la fecha límite (opcional). Formato esperado "AAAA-MM-DD".
 * Las partes se convierten con toIntOrNull(): un texto como "abc" no
 * produce una NumberFormatException, produce null y se informa el error.
 */
fun validarFechaLimite(fecha: String?): String? {
    if (fecha.isNullOrBlank()) return null              // es opcional
    val partes = fecha.split("-")
    if (partes.size != 3) return "La fecha debe tener el formato AAAA-MM-DD"
    val anio = partes[0].toIntOrNull()
    val mes = partes[1].toIntOrNull()
    val dia = partes[2].toIntOrNull()
    return when {
        anio == null || mes == null || dia == null -> "La fecha contiene valores no numéricos"
        mes !in 1..12 -> "El mes debe estar entre 1 y 12"
        dia !in 1..31 -> "El día debe estar entre 1 y 31"
        else -> null
    }
}

/**
 * Valida el recordatorio completo juntando los errores de cada regla.
 * listOfNotNull descarta los null (reglas que pasaron) y deja solo los mensajes.
 */
fun validarRecordatorio(r: Recordatorio): ResultadoValidacion {
    val errores = listOfNotNull(
        validarTitulo(r.titulo),
        validarDescripcion(r.descripcion),
        validarFechaLimite(r.fechaLimite)
    )
    return if (errores.isEmpty()) ResultadoValidacion.Valido
    else ResultadoValidacion.Invalido(errores)
}

/**
 * Crea un recordatorio solo si pasa las validaciones.
 * Devuelve Result (Módulo 1, Cierre: "Result") en lugar de lanzar una
 * excepción al llamador por un dato incorrecto.
 */
fun crearRecordatorio(
    id: Long,
    titulo: String,
    categoria: Categoria,
    prioridad: Prioridad,
    descripcion: String? = null,
    fechaLimite: String? = null,
    ubicacion: String? = null,
    aviso: String? = null
): Result<Recordatorio> {
    // require protege una regla interna (argumento): un id <= 0 sería un error de programación.
    require(id > 0) { "El id debe ser positivo" }

    val candidato = Recordatorio(
        id = id,
        titulo = titulo.trim(),
        categoria = categoria,
        prioridad = prioridad,
        descripcion = descripcion?.trim()?.takeIf { it.isNotEmpty() },
        fechaLimite = fechaLimite,
        ubicacion = ubicacion,
        aviso = aviso
    )
    return when (val resultado = validarRecordatorio(candidato)) {
        ResultadoValidacion.Valido -> Result.success(candidato)
        is ResultadoValidacion.Invalido ->
            Result.failure(IllegalArgumentException(resultado.errores.joinToString("; ")))
    }
}
