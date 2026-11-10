package com.sazloop.reservas.dto;

import com.sazloop.reservas.domain.Reserva.EmpresaDelivery;
import com.sazloop.reservas.domain.Reserva.Modalidad;
import com.sazloop.transacciones.domain.Pago.MedioPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Un pago conjunto que origina reservas independientes. La modalidad (RECOJO o DELIVERY simulado) y la
 * dirección de entrega valen para toda la compra: todos los pedidos del carrito las comparten.
 */
public record CompraMultipleRequest(@NotNull MedioPago medioPago,
        @NotEmpty @Valid List<Pedido> pedidos,
        Modalidad modalidad, EmpresaDelivery empresaDelivery,
        @Size(max = 80) String entregaDistrito, @Size(max = 180) String entregaDireccion,
        @Size(max = 250) String entregaReferencia) {
    /** Compra con recojo en el local (la forma que existía antes del delivery). */
    public CompraMultipleRequest(MedioPago medioPago, List<Pedido> pedidos) {
        this(medioPago, pedidos, null, null, null, null, null);
    }

    public record Pedido(@NotNull Long ofertaId,
            @NotEmpty @Valid List<CompraRequest.Linea> lineas) {}
}
