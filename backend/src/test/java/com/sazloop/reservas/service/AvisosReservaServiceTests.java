package com.sazloop.reservas.service;

import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.service.NotificacionService;
import com.sazloop.identidad.domain.Consumidor;
import com.sazloop.identidad.domain.Direccion;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.domain.PackSorpresa;
import com.sazloop.reservas.domain.CodigoRecojo;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pruebas unitarias de los textos de aviso al consumidor y del cierre de pedidos no recogidos. */
class AvisosReservaServiceTests {
    private NotificacionService notificaciones;
    private AvisosReservaService avisos;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionService.class);
        avisos = new AvisosReservaService(notificaciones);
        usuario = Usuario.builder().id(7L).nombres("Lucía").apellidos("Torres").build();
    }

    private Reserva reserva(LocalDateTime inicio, LocalDateTime fin, Reserva.Modalidad modalidad) {
        Establecimiento local = Establecimiento.builder().id(3L).nombre("Panadería Sol")
                .direccion(Direccion.builder().direccionLinea("Av. Arequipa 100").distrito("Lince").build()).build();
        OfertaPack oferta = OfertaPack.builder().id(5L).inicioRecojo(inicio).finRecojo(fin)
                .packSorpresa(PackSorpresa.builder().establecimiento(local).build()).build();
        Reserva reserva = Reserva.builder().id(11L).oferta(oferta).modalidad(modalidad)
                .consumidor(Consumidor.builder().id(9L).usuario(usuario).build())
                .estado(Reserva.EstadoReserva.CONFIRMADA)
                .codigoRecojo(CodigoRecojo.builder().codigo("ABCD-1234").build()).build();
        if (modalidad == Reserva.Modalidad.DELIVERY) {
            reserva.setEmpresaDelivery(Reserva.EmpresaDelivery.RAPPI);
            reserva.setEntregaDireccion("Jr. Luna 5");
            reserva.setEntregaDistrito("Surco");
        }
        return reserva;
    }

    private String[] aviso(TipoNotificacion tipo) {
        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).crear(same(usuario), eq(tipo), titulo.capture(), mensaje.capture(),
                eq("Reserva"), eq(11L));
        return new String[] {titulo.getValue(), mensaje.getValue()};
    }

    @Test
    void recordatorioDeRecojoEnElMismoDiaMuestraUnSoloDia() {
        avisos.recordatorioRecojo(reserva(LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 20, 30), Reserva.Modalidad.RECOJO));
        String[] aviso = aviso(TipoNotificacion.RECORDATORIO_RECOJO);
        assertEquals("Recoge tu pedido en Panadería Sol", aviso[0]);
        assertEquals("Presenta el código ABCD-1234 en Av. Arequipa 100, Lince, el 10/03/2026 de 18:00 a 20:30. "
                + "Si no lo recoges dentro del horario, el pedido se cerrará sin entrega.", aviso[1]);
    }

    @Test
    void recordatorioQueCruzaLaMedianocheMuestraAmbasFechas() {
        avisos.recordatorioRecojo(reserva(LocalDateTime.of(2026, 3, 10, 22, 0),
                LocalDateTime.of(2026, 3, 11, 1, 15), Reserva.Modalidad.RECOJO));
        String[] aviso = aviso(TipoNotificacion.RECORDATORIO_RECOJO);
        assertTrue(aviso[1].contains("del 10/03/2026 a las 22:00 al 11/03/2026 a las 01:15"), aviso[1]);
    }

    @Test
    void recordatorioDeDeliveryIndicaEmpresaYDireccion() {
        avisos.recordatorioRecojo(reserva(LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 19, 0), Reserva.Modalidad.DELIVERY));
        String[] aviso = aviso(TipoNotificacion.RECORDATORIO_RECOJO);
        assertEquals("Delivery de Panadería Sol", aviso[0]);
        assertTrue(aviso[1].startsWith("Tu pedido llegará con Rappi a Jr. Luna 5, Surco. Código: ABCD-1234."), aviso[1]);
        assertTrue(aviso[1].contains("el 10/03/2026 de 18:00 a 19:00"));
    }

    @Test
    void cancelacionCierraElRecordatorioEInformaElReembolso() {
        Reserva reserva = reserva(LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 19, 0), Reserva.Modalidad.RECOJO);
        avisos.cancelada(reserva, new BigDecimal("4.50"));
        verify(notificaciones).leerPorReferencia(7L, TipoNotificacion.RECORDATORIO_RECOJO, "Reserva", 11L);
        String[] aviso = aviso(TipoNotificacion.CAMBIO_RESERVA);
        assertEquals("Compra cancelada: Panadería Sol", aviso[0]);
        assertTrue(aviso[1].contains("S/ 4.50 (50 % del total)"));
        assertTrue(aviso[1].contains("ABCD-1234 ya no es válido"));
    }

    @Test
    void pedidoNoRecogidoSegunModalidad() {
        avisos.noRecogida(reserva(LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 19, 45), Reserva.Modalidad.RECOJO));
        String[] recojo = aviso(TipoNotificacion.CAMBIO_RESERVA);
        assertEquals("Pedido no recogido: Panadería Sol", recojo[0]);
        assertTrue(recojo[1].startsWith("El horario de recojo terminó el 10/03/2026 a las 19:45 sin entrega."));

        reset(notificaciones);
        avisos.noRecogida(reserva(LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 19, 45), Reserva.Modalidad.DELIVERY));
        String[] delivery = aviso(TipoNotificacion.CAMBIO_RESERVA);
        assertEquals("Pedido no entregado al repartidor: Panadería Sol", delivery[0]);
        assertTrue(delivery[1].contains("el delivery con Rappi no se realizó"));
        verify(notificaciones).leerPorReferencia(7L, TipoNotificacion.RECORDATORIO_RECOJO, "Reserva", 11L);
    }

    @Test
    void efectivoVencidoAvisaQueLasUnidadesSeLiberaron() {
        avisos.efectivoVencido(reserva(LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 19, 0), Reserva.Modalidad.RECOJO));
        String[] aviso = aviso(TipoNotificacion.CAMBIO_RESERVA);
        assertEquals("Reserva en efectivo vencida: Panadería Sol", aviso[0]);
        assertTrue(aviso[1].contains("10 minutos"));
        verify(notificaciones, never()).leerPorReferencia(anyLong(), any(), any(), any());
    }

    @Test
    void elVencimientoIgnoraReservasQueCambiaronAntesDeBloquearlas() {
        ReservaRepository repo = mock(ReservaRepository.class);
        AvisosReservaService avisosMock = mock(AvisosReservaService.class);
        ReservaVencimientoService vencimiento = new ReservaVencimientoService(repo, avisosMock);
        LocalDateTime pasado = LocalDateTime.now().minusHours(1);
        // Otra transacción la entregó entre la consulta y el bloqueo.
        Reserva entregada = reserva(pasado.minusHours(1), pasado, Reserva.Modalidad.RECOJO);
        entregada.setEstado(Reserva.EstadoReserva.RECOGIDA);
        // Otra transacción amplió el horario de la oferta.
        Reserva ampliada = reserva(pasado.minusHours(1), LocalDateTime.now().plusDays(1), Reserva.Modalidad.RECOJO);
        ampliada.setId(12L);
        when(repo.findByEstadoAndOferta_FinRecojoLessThanEqual(eq(Reserva.EstadoReserva.CONFIRMADA), any()))
                .thenReturn(List.of(entregada, ampliada));
        when(repo.bloquear(11L)).thenReturn(Optional.of(entregada));
        when(repo.bloquear(12L)).thenReturn(Optional.of(ampliada));

        vencimiento.vencer();

        assertEquals(Reserva.EstadoReserva.RECOGIDA, entregada.getEstado());
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, ampliada.getEstado());
        assertEquals(CodigoRecojo.EstadoCodigo.PENDIENTE, ampliada.getCodigoRecojo().getEstado());
        verifyNoInteractions(avisosMock);
    }
}
