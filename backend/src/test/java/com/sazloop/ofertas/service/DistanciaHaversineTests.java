package com.sazloop.ofertas.service;

import com.sazloop.identidad.domain.Direccion;
import com.sazloop.identidad.domain.Establecimiento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias del cálculo de distancia de «Cerca de mí» (fórmula de Haversine). */
class DistanciaHaversineTests {

    // Parque Kennedy, Miraflores (Lima)
    private static final double MIRAFLORES_LAT = -12.1219;
    private static final double MIRAFLORES_LNG = -77.0297;

    @Test
    void elMismoPuntoEstaACeroKilometros() {
        Establecimiento local = local(MIRAFLORES_LAT, MIRAFLORES_LNG);
        assertEquals(0.0, OfertaService.distanciaKm(local, MIRAFLORES_LAT, MIRAFLORES_LNG));
    }

    @Test
    void mirafloresASanBorjaEstaAUnosCuatroKilometros() {
        // Centro de San Borja, unos 4 km en línea recta desde el Parque Kennedy
        Establecimiento sanBorja = local(-12.0907, -77.0011);
        Double km = OfertaService.distanciaKm(sanBorja, MIRAFLORES_LAT, MIRAFLORES_LNG);

        assertNotNull(km);
        assertTrue(km > 3.5 && km < 5.0, "Distancia fuera de lo esperado: " + km);
    }

    @Test
    void laDistanciaSeRedondeaADosDecimales() {
        Double km = OfertaService.distanciaKm(local(-12.0907, -77.0011), MIRAFLORES_LAT, MIRAFLORES_LNG);
        assertEquals(km, Math.round(km * 100.0) / 100.0);
    }

    @Test
    void sinUbicacionDelConsumidorNoHayDistancia() {
        Establecimiento local = local(MIRAFLORES_LAT, MIRAFLORES_LNG);
        assertNull(OfertaService.distanciaKm(local, null, MIRAFLORES_LNG));
        assertNull(OfertaService.distanciaKm(local, MIRAFLORES_LAT, null));
    }

    @Test
    void unLocalSinCoordenadasNoTieneDistancia() {
        Establecimiento sinUbicacion = Establecimiento.builder().direccion(Direccion.builder().build()).build();
        assertNull(OfertaService.distanciaKm(sinUbicacion, MIRAFLORES_LAT, MIRAFLORES_LNG));
    }

    private static Establecimiento local(double lat, double lng) {
        Direccion direccion = Direccion.builder()
                .latitud(BigDecimal.valueOf(lat))
                .longitud(BigDecimal.valueOf(lng))
                .build();
        return Establecimiento.builder().direccion(direccion).build();
    }
}
