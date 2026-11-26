package com.sazloop.experiencia.service;

import com.sazloop.experiencia.domain.ImpactoCO2Evitado;
import com.sazloop.experiencia.repository.ImpactoCO2EvitadoRepository;
import com.sazloop.ofertas.domain.VariantePack;
import com.sazloop.ofertas.domain.VariantePack.TamanioVariante;
import com.sazloop.reservas.domain.DetalleReserva;
import com.sazloop.reservas.domain.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pruebas unitarias de la fórmula de impacto ambiental (RF-26): kilos recuperados y CO2 evitado. */
class ImpactoServiceTests {

    private ImpactoCO2EvitadoRepository repositorio;
    private ImpactoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ImpactoCO2EvitadoRepository.class);
        when(repositorio.save(any(ImpactoCO2Evitado.class))).thenAnswer(llamada -> llamada.getArgument(0));
        servicio = new ImpactoService(repositorio);
    }

    @Test
    void dosPacksMedianosSonDosKilosYCincoDeCo2() {
        ImpactoCO2Evitado impacto = servicio.registrar(reserva(linea(TamanioVariante.MEDIANO, 2)));

        assertDecimal("2.0", impacto.getAlimentoRecuperadoKg());
        assertDecimal("5.0", impacto.getCo2EvitadoKg());
        assertTrue(impacto.isEstimacionDisponible());
        assertEquals(ImpactoService.METODOLOGIA, impacto.getMetodologia());
        verify(repositorio).save(impacto);
    }

    @Test
    void sumaTodosLosTamaniosDeLaMismaReserva() {
        // 1 pequeño (0,5) + 1 mediano (1,0) + 2 grandes (3,0) = 4,5 kg; CO2 = 4,5 × 2,5 = 11,25 kg
        ImpactoCO2Evitado impacto = servicio.registrar(reserva(
                linea(TamanioVariante.PEQUENO, 1),
                linea(TamanioVariante.MEDIANO, 1),
                linea(TamanioVariante.GRANDE, 2)));

        assertDecimal("4.5", impacto.getAlimentoRecuperadoKg());
        assertDecimal("11.25", impacto.getCo2EvitadoKg());
    }

    @Test
    void unTamanioDesconocidoUsaUnKiloPorDefecto() {
        ImpactoCO2Evitado impacto = servicio.registrar(reserva(linea(null, 3)));

        assertDecimal("3.0", impacto.getAlimentoRecuperadoKg());
        assertDecimal("7.5", impacto.getCo2EvitadoKg());
    }

    @Test
    void unaReservaSinDetallesRegistraCero() {
        ImpactoCO2Evitado impacto = servicio.registrar(reserva());

        assertDecimal("0", impacto.getAlimentoRecuperadoKg());
        assertDecimal("0", impacto.getCo2EvitadoKg());
    }

    private static Reserva reserva(DetalleReserva... lineas) {
        return Reserva.builder().detalles(new ArrayList<>(List.of(lineas))).build();
    }

    private static DetalleReserva linea(TamanioVariante tamanio, int cantidad) {
        return DetalleReserva.builder()
                .variante(VariantePack.builder().tamanio(tamanio).build())
                .cantidad(cantidad)
                .build();
    }

    private static void assertDecimal(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), () -> "Se esperaba " + esperado + " y salió " + real);
    }
}
