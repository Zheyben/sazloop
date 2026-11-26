package com.sazloop.shared;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj único del sistema: todas las fechas y horas se toman en la hora de Perú, sin importar la zona
 * de la computadora donde corra el servidor. Evita llamar a {@code now()} sin zona explícita.
 */
public final class Tiempo {
    /** Identificador de la zona horaria de todo el sistema. */
    public static final String ID_ZONA = "America/Lima";
    public static final ZoneId ZONA = ZoneId.of(ID_ZONA);

    private Tiempo() {}

    /** Fecha y hora actuales en Lima. */
    public static LocalDateTime ahora() { return LocalDateTime.now(ZONA); }

    /** Fecha actual en Lima. */
    public static LocalDate hoy() { return LocalDate.now(ZONA); }
}
