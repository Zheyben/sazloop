package com.sazloop.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos fallidos de inicio de sesión, compartido por los tres accesos
 * (persona, trabajador y administrador).
 *
 * <p>El contador vive en la memoria del servidor: si se reinicia, vuelve a cero. La clave es el tipo
 * de acceso más el identificador escrito (correo o usuario, en minúsculas y sin espacios); no se
 * cuenta por dirección IP. Tras {@code max-intentos} fallos seguidos la cuenta queda bloqueada
 * {@code bloqueo-minutos} minutos y responde 429 aunque la contraseña sea correcta.
 */
@Component
public class LimiteIntentosLogin {
    public static final String PERSONA = "persona";
    public static final String LOCAL = "local";
    public static final String ADMINISTRADOR = "administrador";
    private static final int TOPE_REGISTROS = 10_000;

    private final int maxIntentos;
    private final long bloqueoMs;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public LimiteIntentosLogin(@Value("${sazloop.login.max-intentos:5}") int maxIntentos,
                               @Value("${sazloop.login.bloqueo-minutos:5}") long bloqueoMinutos) {
        this.maxIntentos = Math.max(1, maxIntentos);
        this.bloqueoMs = Math.max(1, bloqueoMinutos) * 60_000L;
    }

    /** Se llama antes de comprobar la contraseña: si la cuenta está bloqueada, responde 429. */
    public void exigirDisponible(String tipo, String identificador) {
        String clave = clave(tipo, identificador);
        long ahora = System.currentTimeMillis();
        // Pasado el bloqueo, el contador vuelve a cero.
        registros.computeIfPresent(clave, (k, r) -> r.bloqueadoHasta != 0 && ahora >= r.bloqueadoHasta ? null : r);
        Registro registro = registros.get(clave);
        if (registro != null && registro.bloqueadoHasta > ahora)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera " + bloqueoMs / 60_000L
                            + (bloqueoMs == 60_000L ? " minuto" : " minutos") + " e inténtalo de nuevo.");
    }

    public void registrarFallo(String tipo, String identificador) {
        long ahora = System.currentTimeMillis();
        if (registros.size() > TOPE_REGISTROS)
            registros.entrySet().removeIf(e -> e.getValue().bloqueadoHasta != 0 && ahora >= e.getValue().bloqueadoHasta);
        registros.compute(clave(tipo, identificador), (k, r) -> {
            Registro registro = r == null ? new Registro() : r;
            registro.fallos++;
            if (registro.fallos >= maxIntentos) registro.bloqueadoHasta = ahora + bloqueoMs;
            return registro;
        });
    }

    /** Un inicio de sesión correcto pone el contador en cero. */
    public void registrarExito(String tipo, String identificador) {
        registros.remove(clave(tipo, identificador));
    }

    private static String clave(String tipo, String identificador) {
        return tipo + ":" + (identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT));
    }

    private static final class Registro {
        int fallos;
        long bloqueadoHasta;
    }
}
