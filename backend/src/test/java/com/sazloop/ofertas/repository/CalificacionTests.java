package com.sazloop.ofertas.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Promedio de reseñas que la búsqueda calcula a partir de la suma y la cantidad. */
class CalificacionTests {
    @Test
    void sinResenasNoHayPromedio() {
        assertNull(new OfertaBusquedaRepository.Calificacion(0, 0).promedio());
    }

    @Test
    void elPromedioEsLaSumaEntreLaCantidadSinRedondear() {
        assertEquals(4.5, new OfertaBusquedaRepository.Calificacion(9, 2).promedio());
        assertEquals(14.0 / 3, new OfertaBusquedaRepository.Calificacion(14, 3).promedio(), 1e-12);
        assertEquals(5.0, new OfertaBusquedaRepository.Calificacion(5, 1).promedio());
    }
}
