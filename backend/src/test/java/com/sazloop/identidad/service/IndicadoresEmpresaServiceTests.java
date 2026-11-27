package com.sazloop.identidad.service;

import com.sazloop.experiencia.domain.ImpactoCO2Evitado;
import com.sazloop.experiencia.domain.Resena;
import com.sazloop.experiencia.repository.ImpactoCO2EvitadoRepository;
import com.sazloop.experiencia.repository.ResenaRepository;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.identidad.dto.EstablecimientoResponse;
import com.sazloop.reservas.domain.DetalleReserva;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.domain.Reserva.EstadoReserva;
import com.sazloop.reservas.repository.ReservaRepository;
import com.sazloop.transacciones.domain.ComisionPlataforma;
import com.sazloop.transacciones.domain.Pago;
import com.sazloop.transacciones.domain.Pago.EstadoPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del cálculo de indicadores por local y del total de la empresa (con dobles de Mockito). */
class IndicadoresEmpresaServiceTests {
    private static final Long USUARIO = 7L;
    private static final Long LOCAL_A = 1L;
    private static final Long LOCAL_B = 2L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 5, 10, 12, 0);

    private final EmpresaService empresas = mock(EmpresaService.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final ResenaRepository resenas = mock(ResenaRepository.class);
    private final ImpactoCO2EvitadoRepository impactos = mock(ImpactoCO2EvitadoRepository.class);
    private final IndicadoresEmpresaService servicio =
            new IndicadoresEmpresaService(empresas, reservas, resenas, impactos);

    @Test
    void calculaVentasCancelacionesNoRecogidasYEntregasPorLocal() {
        prepararDosLocales();

        var resumen = servicio.resumen(USUARIO);
        var a = resumen.establecimientos().getFirst();

        assertEquals(LOCAL_A, a.establecimientoId());
        assertEquals("Local A", a.establecimiento());
        // Venta recogida + venta pagada y no recogida; la cancelada y las no pagadas no suman.
        assertEquals(2, a.ventas());
        assertIgual("25.00", a.importeVentas());
        assertIgual("2.50", a.comisiones());
        assertIgual("22.50", a.ingresos());
        assertEquals(1, a.cancelaciones());
        assertEquals(1, a.noRecogidas());
        assertEquals(3, a.packsEntregados());
    }

    @Test
    void lasEntregasAntiguasSinValoresCuentanComoCero() {
        prepararDosLocales();

        var a = servicio.resumen(USUARIO).establecimientos().getFirst();

        assertIgual("1.5", a.alimentoRecuperadoKg());
        assertIgual("3.0", a.co2EvitadoKg());
    }

    @Test
    void promediaLasResenasYMuestraSoloLasCincoMasRecientes() {
        prepararDosLocales();

        var a = servicio.resumen(USUARIO).establecimientos().getFirst();

        assertEquals(6, a.cantidadResenas());
        assertIgual("4.00", a.calificacionPromedio());
        assertEquals(5, a.resenasRecientes().size());
        assertEquals("Opinión 0", a.resenasRecientes().getFirst().comentario());
        assertEquals(BASE, a.resenasRecientes().getFirst().fecha());
        assertEquals("Opinión 4", a.resenasRecientes().getLast().comentario());
    }

    @Test
    void unLocalSinMovimientosTieneTodoEnCero() {
        prepararDosLocales();

        var b = servicio.resumen(USUARIO).establecimientos().get(1);

        assertEquals(0, b.ventas());
        assertEquals(0, b.cancelaciones());
        assertIgual("0", b.importeVentas());
        assertIgual("0", b.alimentoRecuperadoKg());
        assertEquals(1, b.cantidadResenas());
        assertIgual("2.00", b.calificacionPromedio());
    }

    @Test
    void elTotalSumaLosLocalesYPonderaElPromedioPorCantidadDeResenas() {
        prepararDosLocales();

        var total = servicio.resumen(USUARIO).total();

        assertNull(total.establecimientoId());
        assertEquals("Total empresa", total.establecimiento());
        assertEquals(2, total.ventas());
        assertIgual("25.00", total.importeVentas());
        assertIgual("2.50", total.comisiones());
        assertIgual("22.50", total.ingresos());
        assertEquals(1, total.cancelaciones());
        assertEquals(1, total.noRecogidas());
        assertEquals(3, total.packsEntregados());
        assertEquals(7, total.cantidadResenas());
        // (4,00 × 6 + 2 × 1) / 7 = 3,71
        assertIgual("3.71", total.calificacionPromedio());
        assertIgual("1.5", total.alimentoRecuperadoKg());
        assertIgual("3.0", total.co2EvitadoKg());
        assertTrue(total.resenasRecientes().isEmpty());
    }

    @Test
    void sinLocalesElTotalQuedaEnCero() {
        when(empresas.listarEstablecimientos(USUARIO)).thenReturn(List.of());

        var resumen = servicio.resumen(USUARIO);

        assertTrue(resumen.establecimientos().isEmpty());
        assertEquals(0, resumen.total().ventas());
        assertEquals(0, resumen.total().cantidadResenas());
        assertIgual("0", resumen.total().calificacionPromedio());
    }

    private void prepararDosLocales() {
        when(empresas.listarEstablecimientos(USUARIO)).thenReturn(List.of(local(LOCAL_A, "Local A"), local(LOCAL_B, "Local B")));

        Reserva recogida = reserva(EstadoReserva.RECOGIDA, "20.00", pago(EstadoPago.CONFIRMADO, "2.00", "18.00"));
        recogida.getDetalles().add(DetalleReserva.builder().cantidad(2).build());
        recogida.getDetalles().add(DetalleReserva.builder().cantidad(1).build());
        Reserva cancelada = reserva(EstadoReserva.CANCELADA, "8.00", pago(EstadoPago.CONFIRMADO, null, null));
        Reserva noRecogida = reserva(EstadoReserva.EXPIRADA, "5.00", pago(EstadoPago.CONFIRMADO, "0.50", "4.50"));
        Reserva expiradaSinPago = reserva(EstadoReserva.EXPIRADA, "7.00", null);
        Reserva pagoPendiente = reserva(EstadoReserva.CONFIRMADA, "9.00", pago(EstadoPago.PENDIENTE, null, null));
        when(reservas.findByOferta_PackSorpresa_Establecimiento_Id(LOCAL_A))
                .thenReturn(List.of(recogida, cancelada, noRecogida, expiradaSinPago, pagoPendiente));
        when(reservas.findByOferta_PackSorpresa_Establecimiento_Id(LOCAL_B)).thenReturn(List.of());

        when(impactos.findByLocal(LOCAL_A)).thenReturn(List.of(
                ImpactoCO2Evitado.builder().alimentoRecuperadoKg(new BigDecimal("1.5")).co2EvitadoKg(new BigDecimal("3.0")).build(),
                ImpactoCO2Evitado.builder().alimentoRecuperadoKg(null).co2EvitadoKg(null).build()));
        when(impactos.findByLocal(LOCAL_B)).thenReturn(List.of());

        int[] notas = {5, 4, 3, 5, 4, 3};
        List<Resena> opiniones = new ArrayList<>();
        // Se entregan desordenadas para comprobar que se ordenan de la más nueva a la más antigua.
        for (int i = notas.length - 1; i >= 0; i--)
            opiniones.add(Resena.builder().calificacion(notas[i]).comentario("Opinión " + i)
                    .fechaPublicacion(BASE.minusDays(i)).build());
        when(resenas.findByEstablecimiento_IdAndVisibleTrue(LOCAL_A)).thenReturn(opiniones);
        when(resenas.findByEstablecimiento_IdAndVisibleTrue(LOCAL_B)).thenReturn(List.of(
                Resena.builder().calificacion(2).comentario("Regular").fechaPublicacion(BASE).build()));
    }

    private static EstablecimientoResponse local(Long id, String nombre) {
        return new EstablecimientoResponse(id, nombre, "Surquillo", "Calle 1", EstadoEstablecimiento.ACTIVO,
                null, null, null);
    }

    private static Reserva reserva(EstadoReserva estado, String monto, Pago pago) {
        return Reserva.builder().estado(estado).montoTotal(new BigDecimal(monto)).pago(pago).build();
    }

    private static Pago pago(EstadoPago estado, String comision, String ingreso) {
        Pago pago = Pago.builder().estado(estado).build();
        if (comision != null)
            pago.setComision(ComisionPlataforma.builder().importeComision(new BigDecimal(comision))
                    .ingresoEstablecimiento(new BigDecimal(ingreso)).build());
        return pago;
    }

    private static void assertIgual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), () -> "se esperaba " + esperado + " y llegó " + real);
    }
}
