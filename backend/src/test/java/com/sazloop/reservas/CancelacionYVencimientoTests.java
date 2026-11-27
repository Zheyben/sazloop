package com.sazloop.reservas;

import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.reservas.domain.CodigoRecojo;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.dto.CompraPresencialRequest;
import com.sazloop.reservas.dto.CompraRequest;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.service.CancelacionService;
import com.sazloop.reservas.service.CompraPresencialService;
import com.sazloop.reservas.service.CompraRecojoService;
import com.sazloop.reservas.service.ReservaVencimientoService;
import com.sazloop.shared.Tiempo;
import com.sazloop.transacciones.domain.Pago;
import com.sazloop.transacciones.domain.Reembolso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cancelación con reembolso del 50 % y cierre automático de pedidos no recogidos. */
class CancelacionYVencimientoTests extends EscenarioReservasBase {
    @Autowired CompraRecojoService compras;
    @Autowired CompraPresencialService presencial;
    @Autowired CancelacionService cancelaciones;
    @Autowired ReservaVencimientoService vencimiento;

    @Test
    void cancelarDevuelveStockReembolsaLaMitadYAvisa() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 2));
        assertEquals(1, stock(oferta.id()));
        cancelaciones.cancelar(consumidor, compra.reservaId());

        assertEquals(3, stock(oferta.id()));
        assertEquals(Reserva.EstadoReserva.CANCELADA, estadoReserva(compra.reservaId()));
        assertEquals(new BigDecimal("8.00"), enTransaccion(compra.reservaId(), r -> r.getCancelacion().getImporteReembolso()));
        assertEquals(new BigDecimal("8.00"), enTransaccion(compra.reservaId(), r -> r.getCancelacion().getImportePenalizacion()));
        assertEquals(Boolean.TRUE, enTransaccion(compra.reservaId(), r -> r.getCancelacion().isStockDevuelto()));
        assertEquals(Reembolso.EstadoReembolso.PROCESADO,
                enTransaccion(compra.reservaId(), r -> r.getCancelacion().getReembolso().getEstado()));
        assertEquals(new BigDecimal("0.80"),
                enTransaccion(compra.reservaId(), r -> r.getPago().getComision().getImporteComision()));
        assertEquals(CodigoRecojo.EstadoCodigo.VENCIDO,
                enTransaccion(compra.reservaId(), r -> r.getCodigoRecojo().getEstado()));
        var aviso = avisos(consumidor, TipoNotificacion.CAMBIO_RESERVA).getFirst();
        assertTrue(aviso.titulo().startsWith("Compra cancelada: "));
        assertTrue(aviso.mensaje().contains("S/ 8.00"));
        assertTrue(avisos(consumidor, TipoNotificacion.RECORDATORIO_RECOJO).getFirst().leida());
        assertEquals(new BigDecimal("8.00"), compras.misCompras(consumidor).getFirst().importeReembolso());
    }

    @Test
    void reservaInexistenteOAjenaDevuelve404() {
        OfertaResponse oferta = oferta(local(), 2);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        falla(HttpStatus.NOT_FOUND, () -> cancelaciones.cancelar(consumidor, Long.MAX_VALUE));
        Long otro = usuario();
        falla(HttpStatus.NOT_FOUND, () -> cancelaciones.cancelar(otro, compra.reservaId()));
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, estadoReserva(compra.reservaId()));
    }

    @Test
    void noSeCancelaUnaCompraPresencialRecogidaOFueraDeHorario() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 5);
        Long consumidor = usuario();
        String token = ofertasRepo.findById(oferta.id()).orElseThrow().getTokenQr();
        CompraResponse qr = presencial.digital(consumidor, new CompraPresencialRequest(token,
                pedido(oferta, Pago.MedioPago.YAPE, 1)));
        falla(HttpStatus.CONFLICT, "Esta compra ya no puede cancelarse", () -> cancelaciones.cancelar(consumidor, qr.reservaId()));

        CompraResponse recogida = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        compras.recoger(local.cuenta(), recogida.codigoRecojo());
        falla(HttpStatus.CONFLICT, "Esta compra ya no puede cancelarse",
                () -> cancelaciones.cancelar(consumidor, recogida.reservaId()));

        CompraResponse tardia = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        finRecojo(oferta.id(), Tiempo.ahora().minusSeconds(1));
        falla(HttpStatus.CONFLICT, "Esta compra ya no puede cancelarse",
                () -> cancelaciones.cancelar(consumidor, tardia.reservaId()));
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, estadoReserva(tardia.reservaId()));
        assertEquals(2, stock(oferta.id()));
    }

    @Test
    void conLocalSuspendidoUOfertaCerradaSeReembolsaPeroNoSeDevuelveStock() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 4);
        Long consumidor = usuario();
        CompraResponse primera = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        CompraResponse segunda = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));

        estadoLocal(local.id(), EstadoEstablecimiento.SUSPENDIDO);
        cancelaciones.cancelar(consumidor, primera.reservaId());
        assertEquals(2, stock(oferta.id()));
        assertEquals(Boolean.FALSE, enTransaccion(primera.reservaId(), r -> r.getCancelacion().isStockDevuelto()));
        assertEquals(Reserva.EstadoReserva.CANCELADA, estadoReserva(primera.reservaId()));

        estadoLocal(local.id(), EstadoEstablecimiento.ACTIVO);
        cambiarOferta(oferta.id(), o -> o.setEstado(OfertaPack.EstadoOferta.FINALIZADA));
        cancelaciones.cancelar(consumidor, segunda.reservaId());
        assertEquals(2, stock(oferta.id()));
        assertEquals(Boolean.FALSE, enTransaccion(segunda.reservaId(), r -> r.getCancelacion().isStockDevuelto()));
        assertEquals(new BigDecimal("4.00"), enTransaccion(segunda.reservaId(), r -> r.getCancelacion().getImporteReembolso()));
    }

    @Test
    void vencerCierraLosPedidosNoRecogidosYAvisaSegunLaModalidad() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        Long otro = usuario();
        CompraResponse recojo = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        CompraResponse delivery = compras.comprar(otro, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                List.of(new CompraRequest.Linea(variante(oferta), 1)), Reserva.Modalidad.DELIVERY,
                Reserva.EmpresaDelivery.PEDIDOSYA, "Lince", "Calle Luna 1", null));
        // Mientras el horario sigue abierto, nada vence.
        vencimiento.vencer();
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, estadoReserva(recojo.reservaId()));

        finRecojo(oferta.id(), Tiempo.ahora().minusSeconds(1));
        vencimiento.vencer();

        assertEquals(Reserva.EstadoReserva.EXPIRADA, estadoReserva(recojo.reservaId()));
        assertEquals(Reserva.EstadoReserva.EXPIRADA, estadoReserva(delivery.reservaId()));
        assertEquals(CodigoRecojo.EstadoCodigo.VENCIDO,
                enTransaccion(recojo.reservaId(), r -> r.getCodigoRecojo().getEstado()));
        var avisoRecojo = avisos(consumidor, TipoNotificacion.CAMBIO_RESERVA).getFirst();
        assertTrue(avisoRecojo.titulo().startsWith("Pedido no recogido: "));
        assertTrue(avisoRecojo.mensaje().contains("no se reembolsa"));
        var avisoDelivery = avisos(otro, TipoNotificacion.CAMBIO_RESERVA).getFirst();
        assertTrue(avisoDelivery.titulo().startsWith("Pedido no entregado al repartidor: "));
        assertTrue(avisoDelivery.mensaje().contains("PedidosYa"));
        assertTrue(avisos(consumidor, TipoNotificacion.RECORDATORIO_RECOJO).getFirst().leida());
        // El pago no se devuelve: sigue confirmado.
        assertEquals(Pago.EstadoPago.CONFIRMADO, enTransaccion(recojo.reservaId(), r -> r.getPago().getEstado()));
    }
}
