package com.sazloop.reservas;

import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.domain.VariantePack.TamanioVariante;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.reservas.domain.CodigoRecojo;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.domain.RetencionStock;
import com.sazloop.reservas.dto.CompraPresencialRequest;
import com.sazloop.reservas.dto.CompraRequest;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.dto.RetencionResponse;
import com.sazloop.reservas.service.CompraPresencialService;
import com.sazloop.reservas.service.CompraRecojoService;
import com.sazloop.shared.Tiempo;
import com.sazloop.transacciones.domain.Pago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Compra presencial con el QR del local: pago digital, reserva en efectivo, confirmación y vencimiento. */
class CompraPresencialServiceTests extends EscenarioReservasBase {
    @Autowired CompraPresencialService presencial;
    @Autowired CompraRecojoService compras;

    private String token(Long ofertaId) {
        return ofertasRepo.findById(ofertaId).orElseThrow().getTokenQr();
    }

    private static CompraRequest delivery(OfertaResponse oferta, Pago.MedioPago medio) {
        return new CompraRequest(oferta.id(), medio, List.of(new CompraRequest.Linea(variante(oferta), 1)),
                Reserva.Modalidad.DELIVERY, Reserva.EmpresaDelivery.RAPPI, "Lince", "Calle", null);
    }

    @Test
    void compraDigitalConQrValidoQuedaComoPresencialYNoCancelable() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        CompraResponse compra = presencial.digital(consumidor,
                new CompraPresencialRequest(token(oferta.id()), pedido(oferta, Pago.MedioPago.YAPE, 2)));
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, compra.estado());
        assertFalse(compra.cancelable());
        assertEquals(Reserva.OrigenCompra.PRESENCIAL_QR, enTransaccion(compra.reservaId(), Reserva::getOrigenCompra));
        assertEquals(Pago.ContextoPago.PRESENCIAL_QR, enTransaccion(compra.reservaId(), r -> r.getPago().getContexto()));
        assertEquals(1, stock(oferta.id()));
    }

    @Test
    void compraDigitalRechazaDeliveryTokenIncorrectoOfertaInexistenteYEfectivo() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        String token = token(oferta.id());
        falla(HttpStatus.BAD_REQUEST, "La compra con QR no admite delivery", () -> presencial.digital(consumidor,
                new CompraPresencialRequest(token, delivery(oferta, Pago.MedioPago.YAPE))));
        falla(HttpStatus.CONFLICT, "QR no válido", () -> presencial.digital(consumidor,
                new CompraPresencialRequest("otro-token", pedido(oferta, Pago.MedioPago.YAPE, 1))));
        falla(HttpStatus.NOT_FOUND, () -> presencial.digital(consumidor, new CompraPresencialRequest(token,
                new CompraRequest(Long.MAX_VALUE, Pago.MedioPago.YAPE, List.of(new CompraRequest.Linea(1L, 1))))));
        falla(HttpStatus.BAD_REQUEST, "Usa la reserva en efectivo", () -> presencial.digital(consumidor,
                new CompraPresencialRequest(token, pedido(oferta, Pago.MedioPago.EFECTIVO, 1))));
        cambiarOferta(oferta.id(), o -> o.setTokenQr(null));
        falla(HttpStatus.CONFLICT, "QR no válido", () -> presencial.digital(consumidor,
                new CompraPresencialRequest(token, pedido(oferta, Pago.MedioPago.YAPE, 1))));
        assertEquals(3, stock(oferta.id()));
        assertTrue(compras.misCompras(consumidor).isEmpty());
    }

    @Test
    void reservaEnEfectivoRetieneStockDiezMinutosYSoloUnaPendientePorConsumidor() {
        OfertaResponse oferta = oferta(local(), 4);
        Long consumidor = usuario();
        String token = token(oferta.id());
        LocalDateTime antes = Tiempo.ahora();
        RetencionResponse retencion = presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(token,
                new CompraRequest(oferta.id(), Pago.MedioPago.EFECTIVO, List.of(
                        new CompraRequest.Linea(variante(oferta), 1), new CompraRequest.Linea(variante(oferta), 1)))));
        assertEquals(new BigDecimal("16.00"), retencion.total());
        assertEquals(2, stock(oferta.id()));
        long minutos = Duration.between(antes.atZone(Tiempo.ZONA), retencion.venceEn().atZone(Tiempo.ZONA)).toMinutes();
        assertTrue(minutos >= 9 && minutos <= 10, "La retención dura 10 minutos: " + minutos);
        assertEquals(Reserva.EstadoReserva.PENDIENTE_PAGO, estadoReserva(retencion.reservaId()));
        List<RetencionStock> holds = retencionesRepo.findByReserva_Id(retencion.reservaId());
        assertEquals(1, holds.size());
        assertEquals(2, holds.getFirst().getCantidad());
        assertEquals(Pago.EstadoPago.PENDIENTE, enTransaccion(retencion.reservaId(), r -> r.getPago().getEstado()));

        falla(HttpStatus.CONFLICT, "Ya tienes una reserva en efectivo pendiente. Confírmala en el local o espera a que venza",
                () -> presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(token,
                        pedido(oferta, Pago.MedioPago.EFECTIVO, 1))));
        assertEquals(2, stock(oferta.id()));
    }

    @Test
    void laRetencionNoPasaDelFinDelHorario() {
        OfertaResponse oferta = oferta(local(), 2);
        LocalDateTime fin = Tiempo.ahora().plusMinutes(3).withNano(0);
        finRecojo(oferta.id(), fin);
        RetencionResponse retencion = presencial.reservarEfectivo(usuario(), new CompraPresencialRequest(
                token(oferta.id()), pedido(oferta, Pago.MedioPago.EFECTIVO, 1)));
        assertEquals(fin, retencion.venceEn());
    }

    @Test
    void reservaEnEfectivoValidaMedioDeliveryOfertaYToken() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 3);
        Long consumidor = usuario();
        String token = token(oferta.id());
        CompraRequest efectivo = pedido(oferta, Pago.MedioPago.EFECTIVO, 1);
        falla(HttpStatus.BAD_REQUEST, "Se requiere efectivo", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, pedido(oferta, Pago.MedioPago.YAPE, 1))));
        falla(HttpStatus.BAD_REQUEST, "La compra con QR no admite delivery", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, delivery(oferta, Pago.MedioPago.EFECTIVO))));
        falla(HttpStatus.NOT_FOUND, () -> presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(token,
                new CompraRequest(Long.MAX_VALUE, Pago.MedioPago.EFECTIVO, efectivo.lineas()))));
        falla(HttpStatus.CONFLICT, "QR no válido para la compra presencial", () -> presencial.reservarEfectivo(
                consumidor, new CompraPresencialRequest("token-falso", efectivo)));

        cambiarOferta(oferta.id(), o -> o.setEstado(OfertaPack.EstadoOferta.AGOTADA));
        falla(HttpStatus.CONFLICT, "Oferta no disponible", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, efectivo)));
        cambiarOferta(oferta.id(), o -> o.setEstado(OfertaPack.EstadoOferta.ACTIVA));
        estadoLocal(local.id(), EstadoEstablecimiento.SUSPENDIDO);
        falla(HttpStatus.CONFLICT, "Oferta no disponible", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, efectivo)));
        estadoLocal(local.id(), EstadoEstablecimiento.ACTIVO);
        finRecojo(oferta.id(), Tiempo.ahora().minusSeconds(1));
        falla(HttpStatus.CONFLICT, "Oferta no disponible", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, efectivo)));
        cambiarOferta(oferta.id(), o -> o.setTokenQr(null));
        falla(HttpStatus.CONFLICT, "QR no válido para la compra presencial", () -> presencial.reservarEfectivo(
                consumidor, new CompraPresencialRequest(token, efectivo)));
        assertEquals(3, stock(oferta.id()));
    }

    @Test
    void reservaEnEfectivoValidaCantidadesStockYConsumidor() {
        OfertaResponse oferta = oferta(local(), List.of(varianteRequest(TamanioVariante.PEQUENO, "6.00", 2)));
        OfertaResponse otra = oferta(local(), 2);
        String token = token(oferta.id());
        Long consumidor = usuario();
        Long varianteId = variante(oferta);
        falla(HttpStatus.BAD_REQUEST, () -> presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(token,
                pedido(oferta, Pago.MedioPago.EFECTIVO, 0))));
        falla(HttpStatus.BAD_REQUEST, () -> presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(token,
                new CompraRequest(oferta.id(), Pago.MedioPago.EFECTIVO, List.of(
                        new CompraRequest.Linea(varianteId, Integer.MAX_VALUE), new CompraRequest.Linea(varianteId, 1))))));
        falla(HttpStatus.CONFLICT, "Stock insuficiente", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, pedido(oferta, Pago.MedioPago.EFECTIVO, 3))));
        falla(HttpStatus.CONFLICT, "Stock insuficiente", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, new CompraRequest(oferta.id(), Pago.MedioPago.EFECTIVO,
                        List.of(new CompraRequest.Linea(variante(otra), 1))))));
        desactivarVariante(oferta.id());
        falla(HttpStatus.CONFLICT, "Stock insuficiente", () -> presencial.reservarEfectivo(consumidor,
                new CompraPresencialRequest(token, pedido(oferta, Pago.MedioPago.EFECTIVO, 1))));

        String tokenOtra = token(otra.id());
        CompraRequest pedidoOtra = pedido(otra, Pago.MedioPago.EFECTIVO, 1);
        falla(HttpStatus.UNAUTHORIZED, () -> presencial.reservarEfectivo(Long.MAX_VALUE,
                new CompraPresencialRequest(tokenOtra, pedidoOtra)));
        Long inactivo = usuario();
        desactivarUsuario(inactivo);
        falla(HttpStatus.UNAUTHORIZED, () -> presencial.reservarEfectivo(inactivo,
                new CompraPresencialRequest(tokenOtra, pedidoOtra)));
        Long bloqueado = usuario();
        consumidorBloqueado(bloqueado);
        falla(HttpStatus.FORBIDDEN, () -> presencial.reservarEfectivo(bloqueado,
                new CompraPresencialRequest(tokenOtra, pedidoOtra)));
        assertEquals(2, stock(oferta.id()));
        assertEquals(2, stock(otra.id()));
    }

    @Test
    void elLocalConfirmaElEfectivoUnaSolaVezYSoloEnSuLocal() {
        Local local = local();
        Local otroLocal = local();
        OfertaResponse oferta = oferta(local, 3);
        Long consumidor = usuario();
        RetencionResponse retencion = presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(
                token(oferta.id()), pedido(oferta, Pago.MedioPago.EFECTIVO, 1)));
        String codigo = retencion.codigoRecojo().toLowerCase();

        falla(HttpStatus.NOT_FOUND, () -> presencial.confirmarEfectivo(local.cuenta(), "NOEXISTE00000000"));
        falla(HttpStatus.NOT_FOUND, () -> presencial.confirmarEfectivo(otroLocal.cuenta(), codigo));

        CompraResponse confirmada = presencial.confirmarEfectivo(local.cuenta(), codigo);
        assertEquals(Reserva.EstadoReserva.RECOGIDA, confirmada.estado());
        assertNotNull(confirmada.numeroBoleta());
        assertEquals(2, stock(oferta.id()));
        assertEquals(RetencionStock.EstadoRetencion.CONFIRMADA,
                retencionesRepo.findByReserva_Id(retencion.reservaId()).getFirst().getEstado());
        assertEquals(Pago.EstadoPago.CONFIRMADO, enTransaccion(retencion.reservaId(), r -> r.getPago().getEstado()));
        assertEquals(Boolean.TRUE, enTransaccion(retencion.reservaId(), r -> r.getPago().getComision().isPendientePorEfectivo()));
        assertEquals(CodigoRecojo.EstadoCodigo.UTILIZADO,
                enTransaccion(retencion.reservaId(), r -> r.getCodigoRecojo().getEstado()));
        falla(HttpStatus.CONFLICT, "No hay efectivo pendiente", () -> presencial.confirmarEfectivo(local.cuenta(), codigo));

        // Una compra digital no tiene efectivo pendiente que confirmar.
        CompraResponse digital = compras.comprar(usuario(), pedido(oferta, Pago.MedioPago.YAPE, 1));
        falla(HttpStatus.CONFLICT, "No hay efectivo pendiente",
                () -> presencial.confirmarEfectivo(local.cuenta(), digital.codigoRecojo()));
    }

    @Test
    void noSeConfirmaUnaRetencionVencidaOSinRetenciones() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 3);
        String token = token(oferta.id());
        RetencionResponse vencida = presencial.reservarEfectivo(usuario(), new CompraPresencialRequest(
                token, pedido(oferta, Pago.MedioPago.EFECTIVO, 1)));
        RetencionStock hold = retencionesRepo.findByReserva_Id(vencida.reservaId()).getFirst();
        hold.setFechaExpiracion(Tiempo.ahora().minusSeconds(1));
        retencionesRepo.saveAndFlush(hold);
        falla(HttpStatus.CONFLICT, "La retención venció",
                () -> presencial.confirmarEfectivo(local.cuenta(), vencida.codigoRecojo()));

        RetencionResponse sinHold = presencial.reservarEfectivo(usuario(), new CompraPresencialRequest(
                token, pedido(oferta, Pago.MedioPago.EFECTIVO, 1)));
        retencionesRepo.deleteAll(retencionesRepo.findByReserva_Id(sinHold.reservaId()));
        falla(HttpStatus.CONFLICT, "La retención venció",
                () -> presencial.confirmarEfectivo(local.cuenta(), sinHold.codigoRecojo()));
        assertEquals(Reserva.EstadoReserva.PENDIENTE_PAGO, estadoReserva(sinHold.reservaId()));
    }

    @Test
    void alVencerLaRetencionSeDevuelveElStockYSeAvisaAlConsumidor() {
        OfertaResponse oferta = oferta(local(), List.of(
                varianteRequest(TamanioVariante.PEQUENO, "6.00", 2),
                varianteRequest(TamanioVariante.GRANDE, "12.00", 2)));
        Long consumidor = usuario();
        RetencionResponse retencion = presencial.reservarEfectivo(consumidor, new CompraPresencialRequest(
                token(oferta.id()), new CompraRequest(oferta.id(), Pago.MedioPago.EFECTIVO, List.of(
                        new CompraRequest.Linea(oferta.variantes().get(0).id(), 1),
                        new CompraRequest.Linea(oferta.variantes().get(1).id(), 2)))));
        List<RetencionStock> holds = retencionesRepo.findByReserva_Id(retencion.reservaId());
        assertEquals(2, holds.size());
        // Una retención ya liberada no devuelve stock dos veces; la otra vence.
        RetencionStock liberada = holds.stream().filter(h -> h.getCantidad() == 2).findFirst().orElseThrow();
        liberada.setEstado(RetencionStock.EstadoRetencion.LIBERADA);
        RetencionStock activa = holds.stream().filter(h -> h.getCantidad() == 1).findFirst().orElseThrow();
        activa.setFechaExpiracion(Tiempo.ahora().minusSeconds(1));
        retencionesRepo.saveAllAndFlush(List.of(liberada, activa));

        presencial.vencerRetenciones();

        assertEquals(Reserva.EstadoReserva.EXPIRADA, estadoReserva(retencion.reservaId()));
        assertEquals(Pago.EstadoPago.RECHAZADO, enTransaccion(retencion.reservaId(), r -> r.getPago().getEstado()));
        assertEquals(CodigoRecojo.EstadoCodigo.VENCIDO,
                enTransaccion(retencion.reservaId(), r -> r.getCodigoRecojo().getEstado()));
        var stocks = transacciones.execute(status -> ofertasRepo.findById(oferta.id()).orElseThrow().getVariantes()
                .stream().collect(java.util.stream.Collectors.toMap(v -> v.getTamanio(), v -> v.getStockDisponible())));
        assertEquals(2, stocks.get(TamanioVariante.PEQUENO));
        assertEquals(0, stocks.get(TamanioVariante.GRANDE));
        var aviso = avisos(consumidor, TipoNotificacion.CAMBIO_RESERVA);
        assertEquals(1, aviso.size());
        assertTrue(aviso.getFirst().titulo().startsWith("Reserva en efectivo vencida: "));
    }

    @Test
    void unaRetencionVencidaDeUnaReservaYaCerradaNoTocaElStock() {
        OfertaResponse oferta = oferta(local(), 3);
        RetencionResponse retencion = presencial.reservarEfectivo(usuario(), new CompraPresencialRequest(
                token(oferta.id()), pedido(oferta, Pago.MedioPago.EFECTIVO, 1)));
        Reserva reserva = reservasRepo.findById(retencion.reservaId()).orElseThrow();
        reserva.setEstado(Reserva.EstadoReserva.CANCELADA);
        reservasRepo.saveAndFlush(reserva);
        RetencionStock hold = retencionesRepo.findByReserva_Id(retencion.reservaId()).getFirst();
        hold.setFechaExpiracion(Tiempo.ahora().minusSeconds(1));
        retencionesRepo.saveAndFlush(hold);

        presencial.vencerRetenciones();

        assertEquals(Reserva.EstadoReserva.CANCELADA, estadoReserva(retencion.reservaId()));
        assertEquals(RetencionStock.EstadoRetencion.ACTIVA,
                retencionesRepo.findByReserva_Id(retencion.reservaId()).getFirst().getEstado());
        assertEquals(2, stock(oferta.id()));
    }
}
