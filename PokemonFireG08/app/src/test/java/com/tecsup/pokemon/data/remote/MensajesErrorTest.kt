package com.tecsup.pokemon.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MensajesErrorTest {

    @Test fun error404IndicaQueNoSeEncontro() {
        assertEquals("No se encontró la información en PokeAPI (error 404).", mensajeHttp(404))
    }

    @Test fun errores5xxIndicanProblemaDelServidorConSuCodigo() {
        assertTrue(mensajeHttp(500).contains("servidor de PokeAPI"))
        assertTrue(mensajeHttp(503).contains("503"))
    }

    @Test fun demasiadasSolicitudesPideEsperar() {
        assertTrue(mensajeHttp(429).contains("Espera un momento"))
    }

    @Test fun codigoNoContempladoMuestraElCodigo() {
        assertEquals("El servidor respondió con error HTTP 403.", mensajeHttp(403))
    }

    @Test fun timeoutSeDistingueDeNoTenerInternet() {
        assertTrue(mensajeConexion(SocketTimeoutException("lento")).contains("tardó demasiado"))
        assertEquals("No se pudo conectar. Revisa tu conexión a Internet.", mensajeConexion(IOException("sin red")))
    }
}
