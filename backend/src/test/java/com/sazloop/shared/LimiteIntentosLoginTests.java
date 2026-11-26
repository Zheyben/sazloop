package com.sazloop.shared;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias del límite de intentos de inicio de sesión (sin Spring ni base de datos). */
class LimiteIntentosLoginTests {

    private static final String CORREO = "persona@sazloop.test";

    @Test
    void permiteIngresarMientrasNoLlegueAlTope() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(3, 5);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);
        assertDoesNotThrow(() -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
    }

    @Test
    void bloqueaCon429AlLlegarAlTopeDeFallos() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(3, 5);
        for (int i = 0; i < 3; i++) limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
        assertTrue(error.getReason().contains("5 minutos"));
    }

    @Test
    void elMensajeUsaSingularConUnMinutoDeBloqueo() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(1, 1);
        limite.registrarFallo(LimiteIntentosLogin.LOCAL, "demo.local");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> limite.exigirDisponible(LimiteIntentosLogin.LOCAL, "demo.local"));
        assertTrue(error.getReason().contains("1 minuto "));
    }

    @Test
    void ignoraMayusculasYEspaciosDelIdentificador() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(2, 5);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, "  Persona@SAZLOOP.test ");
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);

        assertThrows(ResponseStatusException.class,
                () -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
    }

    @Test
    void unIngresoCorrectoPoneElContadorEnCero() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(2, 5);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);
        limite.registrarExito(LimiteIntentosLogin.PERSONA, CORREO);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);

        assertDoesNotThrow(() -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
    }

    @Test
    void cadaTipoDeAccesoLlevaSuPropioContador() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(1, 5);
        limite.registrarFallo(LimiteIntentosLogin.ADMINISTRADOR, CORREO);

        assertThrows(ResponseStatusException.class,
                () -> limite.exigirDisponible(LimiteIntentosLogin.ADMINISTRADOR, CORREO));
        assertDoesNotThrow(() -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
    }

    @Test
    void valoresInvalidosDeConfiguracionUsanElMinimoDeUno() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(0, 0);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, null));
        assertTrue(error.getReason().contains("1 minuto "));
    }
}
