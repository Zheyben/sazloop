package com.sazloop.reservas;

import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.dto.ResenaRequest;
import com.sazloop.experiencia.service.ResenaService;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.domain.VariantePack.TamanioVariante;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.reservas.domain.CodigoRecojo;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.dto.CompraMultipleRequest;
import com.sazloop.reservas.dto.CompraRequest;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.repository.CodigoRecojoRepository;
import com.sazloop.reservas.service.CancelacionService;
import com.sazloop.reservas.service.CompraRecojoService;
import com.sazloop.shared.Tiempo;
import com.sazloop.transacciones.domain.Pago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/** Compra remota: lotes, límites de stock, ofertas no disponibles, delivery, medios de pago y recojo. */
class CompraRecojoServiceTests extends EscenarioReservasBase {
    @Autowired CompraRecojoService compras;
    @Autowired CancelacionService cancelaciones;
    @Autowired ResenaService resenas;

    @Test
    void elEfectivoNoEsUnPagoRemotoNiSueltoNiEnLote() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        falla(HttpStatus.BAD_REQUEST, "El efectivo no es un pago remoto",
                () -> compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.EFECTIVO, 1)));
        falla(HttpStatus.BAD_REQUEST, "El efectivo no es un pago remoto",
                () -> compras.comprarVarios(consumidor, new CompraMultipleRequest(Pago.MedioPago.EFECTIVO,
                        List.of(new CompraMultipleRequest.Pedido(oferta.id(),
                                List.of(new CompraRequest.Linea(variante(oferta), 1)))))));
        assertEquals(3, stock(oferta.id()));
        assertTrue(compras.misCompras(consumidor).isEmpty());
    }

    @Test
    void conTarjetaDeDebitoLaCompraQuedaConfirmadaConBoletaYCodigo() {
        OfertaResponse oferta = oferta(local(), 2);
        Long consumidor = usuario("Ana María", "Quispe");
        CompraResponse compra = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.TARJETA_DEBITO, 2));
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, compra.estado());
        assertEquals("TARJETA_DEBITO", compra.medioPago());
        assertEquals(new BigDecimal("16.00"), compra.total());
        assertTrue(compra.numeroBoleta().startsWith("SIM-"));
        assertTrue(compra.cancelable());
        assertNull(compra.importeReembolso());
        assertEquals("Ana Q.", compra.cliente());
        assertEquals(Reserva.Modalidad.RECOJO, compra.modalidad());
        assertNull(compra.empresaDelivery());
        assertEquals(0, stock(oferta.id()));
        // El recordatorio de recojo indica dónde presentar el código.
        var recordatorio = avisos(consumidor, TipoNotificacion.RECORDATORIO_RECOJO);
        assertEquals(1, recordatorio.size());
        assertTrue(recordatorio.getFirst().titulo().startsWith("Recoge tu pedido en "));
        assertTrue(recordatorio.getFirst().mensaje().contains(compra.codigoRecojo()));
        assertTrue(recordatorio.getFirst().mensaje().contains("Av. Prueba 123, Lince"));
    }

    @Test
    void ofertaInexistenteDevuelve404() {
        Long consumidor = usuario();
        CompraRequest pedido = new CompraRequest(Long.MAX_VALUE, Pago.MedioPago.YAPE, List.of(new CompraRequest.Linea(1L, 1)));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> compras.comprar(consumidor, pedido));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void ofertaFinalizadaLocalSuspendidoUHorarioTerminadoNoSeVenden() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 3);
        Long consumidor = usuario();
        CompraRequest pedido = pedido(oferta, Pago.MedioPago.YAPE, 1);

        cambiarOferta(oferta.id(), o -> o.setEstado(OfertaPack.EstadoOferta.FINALIZADA));
        falla(HttpStatus.CONFLICT, "Oferta no disponible", () -> compras.comprar(consumidor, pedido));
        cambiarOferta(oferta.id(), o -> o.setEstado(OfertaPack.EstadoOferta.ACTIVA));

        estadoLocal(local.id(), EstadoEstablecimiento.SUSPENDIDO);
        falla(HttpStatus.CONFLICT, "Oferta no disponible", () -> compras.comprar(consumidor, pedido));
        estadoLocal(local.id(), EstadoEstablecimiento.ACTIVO);

        finRecojo(oferta.id(), Tiempo.ahora().minusSeconds(1));
        falla(HttpStatus.CONFLICT, "Oferta no disponible", () -> compras.comprar(consumidor, pedido));
        assertEquals(3, stock(oferta.id()));
        assertTrue(compras.misCompras(consumidor).isEmpty());
    }

    @Test
    void cantidadesInvalidasODesbordadasSeRechazanCon400() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        falla(HttpStatus.BAD_REQUEST, "Cantidad inválida",
                () -> compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 0)));
        falla(HttpStatus.BAD_REQUEST, "Cantidad inválida", () -> compras.comprar(consumidor,
                new CompraRequest(oferta.id(), Pago.MedioPago.YAPE, List.of(
                        new CompraRequest.Linea(variante(oferta), Integer.MAX_VALUE),
                        new CompraRequest.Linea(variante(oferta), 1)))));
        assertEquals(3, stock(oferta.id()));
    }

    @Test
    void lineasRepetidasDeLaMismaVarianteSeSumanYDescuentanJuntas() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                List.of(new CompraRequest.Linea(variante(oferta), 1), new CompraRequest.Linea(variante(oferta), 2))));
        assertEquals(new BigDecimal("24.00"), compra.total());
        assertEquals(1, compra.lineas().size());
        assertEquals(3, compra.lineas().getFirst().cantidad());
        assertEquals(0, stock(oferta.id()));
    }

    @Test
    void stockInsuficienteVarianteDeOtraOfertaOVarianteInactivaDevuelven409() {
        OfertaResponse oferta = oferta(local(), List.of(
                varianteRequest(TamanioVariante.PEQUENO, "6.00", 2),
                varianteRequest(TamanioVariante.GRANDE, "12.00", 1)));
        OfertaResponse otra = oferta(local(), 5);
        Long consumidor = usuario();
        String mensaje = "Stock insuficiente o variante ajena a la oferta";

        falla(HttpStatus.CONFLICT, mensaje, () -> compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 3)));
        falla(HttpStatus.CONFLICT, mensaje, () -> compras.comprar(consumidor, new CompraRequest(oferta.id(),
                Pago.MedioPago.YAPE, List.of(new CompraRequest.Linea(variante(otra), 1)))));
        desactivarVariante(otra.id());
        falla(HttpStatus.CONFLICT, mensaje, () -> compras.comprar(consumidor, pedido(otra, Pago.MedioPago.YAPE, 1)));
        assertEquals(5, stock(otra.id()));

        // Dos variantes de la misma oferta en un solo pedido.
        CompraResponse mixta = compras.comprar(consumidor, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                List.of(new CompraRequest.Linea(oferta.variantes().get(0).id(), 2),
                        new CompraRequest.Linea(oferta.variantes().get(1).id(), 1))));
        assertEquals(new BigDecimal("24.00"), mixta.total());
        assertEquals(2, mixta.lineas().size());
        assertEquals(Boolean.TRUE, transacciones.execute(status -> ofertasRepo.findById(oferta.id()).orElseThrow()
                .getVariantes().stream().allMatch(v -> v.getStockDisponible() == 0)));
    }

    @Test
    void usuarioInexistenteOInactivoDa401YConsumidorBloqueadoDa403() {
        OfertaResponse oferta = oferta(local(), 3);
        CompraRequest pedido = pedido(oferta, Pago.MedioPago.YAPE, 1);
        falla(HttpStatus.UNAUTHORIZED, () -> compras.comprar(Long.MAX_VALUE, pedido));

        Long inactivo = usuario();
        desactivarUsuario(inactivo);
        falla(HttpStatus.UNAUTHORIZED, () -> compras.comprar(inactivo, pedido));

        Long bloqueado = usuario();
        consumidorBloqueado(bloqueado);
        falla(HttpStatus.FORBIDDEN, () -> compras.comprar(bloqueado, pedido));
        assertEquals(3, stock(oferta.id()));
    }

    @Test
    void loteDeVariasOfertasCreaUnaReservaPorPedidoYUnaFallaRevierteTodo() {
        OfertaResponse primera = oferta(local(), 2);
        OfertaResponse segunda = oferta(local(), 1);
        Long consumidor = usuario();
        List<CompraResponse> lote = compras.comprarVarios(consumidor, new CompraMultipleRequest(Pago.MedioPago.YAPE,
                List.of(new CompraMultipleRequest.Pedido(primera.id(), List.of(new CompraRequest.Linea(variante(primera), 1))),
                        new CompraMultipleRequest.Pedido(segunda.id(), List.of(new CompraRequest.Linea(variante(segunda), 1))))));
        assertEquals(2, lote.size());
        assertNotEquals(lote.get(0).establecimientoId(), lote.get(1).establecimientoId());
        assertEquals(1, stock(primera.id()));
        assertEquals(0, stock(segunda.id()));

        // La segunda oferta ya no tiene stock: la primera tampoco debe descontarse.
        falla(HttpStatus.CONFLICT, () -> compras.comprarVarios(consumidor, new CompraMultipleRequest(Pago.MedioPago.YAPE,
                List.of(new CompraMultipleRequest.Pedido(primera.id(), List.of(new CompraRequest.Linea(variante(primera), 1))),
                        new CompraMultipleRequest.Pedido(segunda.id(), List.of(new CompraRequest.Linea(variante(segunda), 1)))))));
        assertEquals(1, stock(primera.id()));
        assertEquals(2, compras.misCompras(consumidor).size());
    }

    @Test
    void deliveryGuardaEmpresaYDireccionRecortadasYAvisaAlConsumidor() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                List.of(new CompraRequest.Linea(variante(oferta), 1)), Reserva.Modalidad.DELIVERY,
                Reserva.EmpresaDelivery.RAPPI, "  Miraflores ", " Calle Sol 456 ", "   "));
        assertEquals(Reserva.Modalidad.DELIVERY, compra.modalidad());
        assertEquals(Reserva.EmpresaDelivery.RAPPI, compra.empresaDelivery());
        assertEquals("Miraflores", compra.entregaDistrito());
        assertEquals("Calle Sol 456", compra.entregaDireccion());
        assertNull(compra.entregaReferencia());
        var recordatorio = avisos(consumidor, TipoNotificacion.RECORDATORIO_RECOJO).getFirst();
        assertTrue(recordatorio.titulo().startsWith("Delivery de "));
        assertTrue(recordatorio.mensaje().contains("Rappi a Calle Sol 456, Miraflores"));

        CompraResponse conReferencia = compras.comprar(consumidor, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                List.of(new CompraRequest.Linea(variante(oferta), 1)), Reserva.Modalidad.DELIVERY,
                Reserva.EmpresaDelivery.PEDIDOSYA, "Lince", "Jr. Uno 1", " Puerta azul "));
        assertEquals("Puerta azul", conReferencia.entregaReferencia());
        assertEquals(1, stock(oferta.id()));
    }

    @Test
    void deliveryEnLoteSeAplicaATodosLosPedidos() {
        OfertaResponse oferta = oferta(local(), 2);
        Long consumidor = usuario();
        List<CompraResponse> lote = compras.comprarVarios(consumidor, new CompraMultipleRequest(Pago.MedioPago.YAPE,
                List.of(new CompraMultipleRequest.Pedido(oferta.id(), List.of(new CompraRequest.Linea(variante(oferta), 1))),
                        new CompraMultipleRequest.Pedido(oferta.id(), List.of(new CompraRequest.Linea(variante(oferta), 1)))),
                Reserva.Modalidad.DELIVERY, Reserva.EmpresaDelivery.INDRIVE, "Surco", "Av. Dos 2", null));
        assertTrue(lote.stream().allMatch(c -> c.modalidad() == Reserva.Modalidad.DELIVERY
                && c.empresaDelivery() == Reserva.EmpresaDelivery.INDRIVE && "Surco".equals(c.entregaDistrito())));
    }

    @Test
    void deliveryExigeEmpresaDistritoYDireccion() {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        List<CompraRequest.Linea> lineas = List.of(new CompraRequest.Linea(variante(oferta), 1));
        Reserva.Modalidad delivery = Reserva.Modalidad.DELIVERY;
        Reserva.EmpresaDelivery rappi = Reserva.EmpresaDelivery.RAPPI;
        falla(HttpStatus.BAD_REQUEST, "Elige la empresa de delivery", () -> compras.comprar(consumidor,
                new CompraRequest(oferta.id(), Pago.MedioPago.YAPE, lineas, delivery, null, "Lince", "Calle", null)));
        falla(HttpStatus.BAD_REQUEST, "Indica el distrito de entrega", () -> compras.comprar(consumidor,
                new CompraRequest(oferta.id(), Pago.MedioPago.YAPE, lineas, delivery, rappi, null, "Calle", null)));
        falla(HttpStatus.BAD_REQUEST, "Indica el distrito de entrega", () -> compras.comprar(consumidor,
                new CompraRequest(oferta.id(), Pago.MedioPago.YAPE, lineas, delivery, rappi, "  ", "Calle", null)));
        falla(HttpStatus.BAD_REQUEST, "Indica la dirección de entrega", () -> compras.comprar(consumidor,
                new CompraRequest(oferta.id(), Pago.MedioPago.YAPE, lineas, delivery, rappi, "Lince", null, null)));
        falla(HttpStatus.BAD_REQUEST, "Indica la dirección de entrega", () -> compras.comprar(consumidor,
                new CompraRequest(oferta.id(), Pago.MedioPago.YAPE, lineas, delivery, rappi, "Lince", " ", null)));
        // Con modalidad RECOJO explícita no se exige dirección.
        CompraResponse recojo = compras.comprar(consumidor, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                lineas, Reserva.Modalidad.RECOJO, null, null, null, null));
        assertEquals(Reserva.Modalidad.RECOJO, recojo.modalidad());
        assertEquals(2, stock(oferta.id()));
    }

    @Test
    void elTrabajadorBuscaElCodigoSinGuionesYEnMinusculasSoloEnSuLocal() {
        Local local = local();
        Local otroLocal = local();
        OfertaResponse oferta = oferta(local, 2);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        String abreviado = compra.codigoRecojo().replace("-", "").toLowerCase();
        assertEquals(compra.reservaId(), compras.consultarCodigo(local.cuenta(), abreviado).reservaId());
        assertEquals(compra.reservaId(), compras.consultarCodigo(local.cuenta(), " " + compra.codigoRecojo() + " ").reservaId());
        falla(HttpStatus.NOT_FOUND, () -> compras.consultarCodigo(otroLocal.cuenta(), compra.codigoRecojo()));
        falla(HttpStatus.NOT_FOUND, () -> compras.consultarCodigo(local.cuenta(), "NOEXISTE00000000"));
        falla(HttpStatus.NOT_FOUND, () -> compras.recoger(otroLocal.cuenta(), compra.codigoRecojo()));
        falla(HttpStatus.NOT_FOUND, () -> compras.recoger(local.cuenta(), "NOEXISTE00000000"));
        assertEquals("", CodigoRecojoRepository.normalizar(null));
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, estadoReserva(compra.reservaId()));
    }

    @Test
    void recogerMarcaElCodigoUsadoYNoPermiteUnSegundoUso() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 2);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        CompraResponse recogida = compras.recoger(local.cuenta(), compra.codigoRecojo().toLowerCase());
        assertEquals(Reserva.EstadoReserva.RECOGIDA, recogida.estado());
        assertTrue(recogida.resenaPendiente());
        assertFalse(recogida.cancelable());
        assertEquals(CodigoRecojo.EstadoCodigo.UTILIZADO,
                enTransaccion(compra.reservaId(), r -> r.getCodigoRecojo().getEstado()));
        assertNotNull(enTransaccion(compra.reservaId(), r -> r.getRecojo()));
        assertTrue(avisos(consumidor, TipoNotificacion.RECORDATORIO_RECOJO).getFirst().leida());
        falla(HttpStatus.CONFLICT, "El código ya fue usado o la compra no está confirmada",
                () -> compras.recoger(local.cuenta(), compra.codigoRecojo()));
    }

    @Test
    void noSeEntregaUnaCompraCanceladaNiFueraDelHorario() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 3);
        Long consumidor = usuario();
        CompraResponse cancelada = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        cancelaciones.cancelar(consumidor, cancelada.reservaId());
        falla(HttpStatus.CONFLICT, "El código ya fue usado o la compra no está confirmada",
                () -> compras.recoger(local.cuenta(), cancelada.codigoRecojo()));

        CompraResponse tardia = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        finRecojo(oferta.id(), Tiempo.ahora().minusSeconds(1));
        falla(HttpStatus.CONFLICT, "El horario de recojo de este pedido ya terminó",
                () -> compras.recoger(local.cuenta(), tardia.codigoRecojo()));
        assertEquals(Reserva.EstadoReserva.CONFIRMADA, estadoReserva(tardia.reservaId()));
        assertFalse(compras.misCompras(consumidor).getFirst().cancelable());
    }

    @Test
    void laResenaYaHechaEnElLocalQuitaLaResenaPendienteDeOtrosPedidos() {
        Local local = local();
        OfertaResponse oferta = oferta(local, 3);
        Long consumidor = usuario();
        CompraResponse primera = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        CompraResponse segunda = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        compras.recoger(local.cuenta(), primera.codigoRecojo());
        compras.recoger(local.cuenta(), segunda.codigoRecojo());
        assertTrue(compras.misCompras(consumidor).stream().allMatch(CompraResponse::resenaPendiente));
        resenas.registrar(consumidor, primera.reservaId(), new ResenaRequest(4, "Bien"));
        // Una reseña por local: ninguna de las dos compras queda pendiente.
        assertTrue(compras.misCompras(consumidor).stream().noneMatch(CompraResponse::resenaPendiente));
    }
}
