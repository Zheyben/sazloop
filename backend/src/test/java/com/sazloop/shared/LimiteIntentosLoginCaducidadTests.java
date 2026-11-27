package com.sazloop.shared;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Caducidad del bloqueo y limpieza de registros del límite de intentos. Para no esperar minutos reales,
 * se adelanta el vencimiento del bloqueo cambiando por reflexión la hora guardada en el registro.
 */
class LimiteIntentosLoginCaducidadTests {
    private static final String CORREO = "caducidad@sazloop.test";

    @Test
    void pasadoElBloqueoElContadorVuelveACero() throws Exception {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(2, 5);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);
        assertThrows(ResponseStatusException.class, () -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));

        vencerBloqueo(limite, "persona:" + CORREO);

        assertDoesNotThrow(() -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
        assertFalse(registros(limite).containsKey("persona:" + CORREO), "el registro vencido se elimina");
        // Tras el desbloqueo vuelve a contar desde cero: un fallo no basta para bloquear.
        limite.registrarFallo(LimiteIntentosLogin.PERSONA, CORREO);
        assertDoesNotThrow(() -> limite.exigirDisponible(LimiteIntentosLogin.PERSONA, CORREO));
    }

    @Test
    void conDemasiadosRegistrosSeBorranSoloLosBloqueosVencidos() throws Exception {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(2, 5);
        // Un bloqueo vencido, un bloqueo vigente y más de 10 000 identificadores con un solo fallo.
        limite.registrarFallo(LimiteIntentosLogin.LOCAL, "vencido");
        limite.registrarFallo(LimiteIntentosLogin.LOCAL, "vencido");
        vencerBloqueo(limite, "local:vencido");
        limite.registrarFallo(LimiteIntentosLogin.LOCAL, "vigente");
        limite.registrarFallo(LimiteIntentosLogin.LOCAL, "vigente");
        for (int i = 0; i < 10_000; i++) limite.registrarFallo(LimiteIntentosLogin.LOCAL, "usuario-" + i);

        limite.registrarFallo(LimiteIntentosLogin.LOCAL, "nuevo");

        Map<String, ?> registros = registros(limite);
        assertFalse(registros.containsKey("local:vencido"));
        assertTrue(registros.containsKey("local:vigente"));
        assertTrue(registros.containsKey("local:usuario-0"));
        assertTrue(registros.containsKey("local:nuevo"));
        assertThrows(ResponseStatusException.class, () -> limite.exigirDisponible(LimiteIntentosLogin.LOCAL, "vigente"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> registros(LimiteIntentosLogin limite) throws Exception {
        Field campo = LimiteIntentosLogin.class.getDeclaredField("registros");
        campo.setAccessible(true);
        return (Map<String, Object>) campo.get(limite);
    }

    private static void vencerBloqueo(LimiteIntentosLogin limite, String clave) throws Exception {
        Object registro = registros(limite).get(clave);
        Field hasta = registro.getClass().getDeclaredField("bloqueadoHasta");
        hasta.setAccessible(true);
        hasta.setLong(registro, System.currentTimeMillis() - 1);
    }
}
