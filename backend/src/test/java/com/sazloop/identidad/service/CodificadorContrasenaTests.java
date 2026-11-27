package com.sazloop.identidad.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias del hash PBKDF2 de contraseñas (sin Spring ni base de datos). */
class CodificadorContrasenaTests {
    private final CodificadorContrasena codificador = new CodificadorContrasena();

    @Test
    void codificaConFormatoPbkdf2YSalDistintaCadaVez() {
        String primero = codificador.codificar("ClaveSegura1");
        String segundo = codificador.codificar("ClaveSegura1");

        String[] partes = primero.split("\\$");
        assertEquals(4, partes.length);
        assertEquals("pbkdf2-sha256", partes[0]);
        assertEquals("210000", partes[1]);
        assertNotEquals(primero, segundo, "cada hash debe llevar su propia sal");
        assertFalse(primero.contains("ClaveSegura1"));
    }

    @Test
    void verificaLaContrasenaCorrectaYRechazaLaIncorrecta() {
        String hash = codificador.codificar("ClaveSegura1");

        assertTrue(codificador.coincide("ClaveSegura1", hash));
        assertFalse(codificador.coincide("ClaveSegura2", hash));
        assertFalse(codificador.coincide("", hash));
    }

    @Test
    void rechazaValoresNulos() {
        String hash = codificador.codificar("ClaveSegura1");

        assertFalse(codificador.coincide(null, hash));
        assertFalse(codificador.coincide("ClaveSegura1", null));
    }

    @Test
    void rechazaHashesMalFormados() {
        String hash = codificador.codificar("ClaveSegura1");
        String[] partes = hash.split("\\$");

        // Cantidad de partes incorrecta.
        assertFalse(codificador.coincide("ClaveSegura1", "texto-plano"));
        assertFalse(codificador.coincide("ClaveSegura1", hash + "$extra"));
        // Algoritmo desconocido.
        assertFalse(codificador.coincide("ClaveSegura1", "md5$" + partes[1] + "$" + partes[2] + "$" + partes[3]));
        // Iteraciones no numéricas, cero o negativas.
        assertFalse(codificador.coincide("ClaveSegura1", partes[0] + "$muchas$" + partes[2] + "$" + partes[3]));
        assertFalse(codificador.coincide("ClaveSegura1", partes[0] + "$0$" + partes[2] + "$" + partes[3]));
        assertFalse(codificador.coincide("ClaveSegura1", partes[0] + "$-5$" + partes[2] + "$" + partes[3]));
        // Sal o hash que no son Base64.
        assertFalse(codificador.coincide("ClaveSegura1", partes[0] + "$" + partes[1] + "$%%%$" + partes[3]));
        assertFalse(codificador.coincide("ClaveSegura1", partes[0] + "$" + partes[1] + "$" + partes[2] + "$***"));
    }

    @Test
    void conOtraCantidadDeIteracionesElHashNoCoincide() {
        String hash = codificador.codificar("ClaveSegura1");
        String[] partes = hash.split("\\$");

        assertFalse(codificador.coincide("ClaveSegura1", partes[0] + "$1000$" + partes[2] + "$" + partes[3]));
    }

    @Test
    void rellenarSiempreDevuelveFalso() {
        assertFalse(codificador.rellenar("ClaveSegura1"));
        assertFalse(codificador.rellenar(""));
        assertFalse(codificador.rellenar(null));
    }
}
