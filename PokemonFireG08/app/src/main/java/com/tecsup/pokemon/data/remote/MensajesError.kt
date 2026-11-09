package com.tecsup.pokemon.data.remote

import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Convierte los errores de red en mensajes que el usuario entiende.
 * Siempre incluye el código HTTP para poder diagnosticar el problema.
 */
internal fun mensajeHttp(codigo: Int): String = when (codigo) {
    404 -> "No se encontró la información en PokeAPI (error 404)."
    408 -> "PokeAPI tardó demasiado en responder (error 408). Inténtalo de nuevo."
    429 -> "Se hicieron demasiadas solicitudes a PokeAPI (error 429). Espera un momento."
    in 500..599 -> "El servidor de PokeAPI tiene problemas (error $codigo). Inténtalo más tarde."
    else -> "El servidor respondió con error HTTP $codigo."
}

/** Distingue una conexión lenta (timeout) de no tener internet. */
internal fun mensajeConexion(e: IOException): String =
    if (e is SocketTimeoutException) {
        "PokeAPI tardó demasiado en responder. Revisa tu conexión e inténtalo de nuevo."
    } else {
        "No se pudo conectar. Revisa tu conexión a Internet."
    }
