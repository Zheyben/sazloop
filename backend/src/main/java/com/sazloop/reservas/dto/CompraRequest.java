package com.sazloop.reservas.dto;

import com.sazloop.reservas.domain.Reserva.EmpresaDelivery;
import com.sazloop.reservas.domain.Reserva.Modalidad;
import com.sazloop.transacciones.domain.Pago.MedioPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Compra remota con pago simulado; cada variante se descuenta de su stock.
 * La modalidad es opcional: si no se manda, es RECOJO. Con DELIVERY (simulado) se exige la empresa, el
 * distrito y la dirección de entrega; la referencia es opcional.
 */
public record CompraRequest(@NotNull Long ofertaId, @NotNull MedioPago medioPago,
        @NotEmpty List<@Valid Linea> lineas,
        Modalidad modalidad, EmpresaDelivery empresaDelivery,
        @Size(max = 80) String entregaDistrito, @Size(max = 180) String entregaDireccion,
        @Size(max = 250) String entregaReferencia) {
    /** Compra con recojo en el local (la forma que existía antes del delivery). */
    public CompraRequest(Long ofertaId, MedioPago medioPago, List<Linea> lineas) {
        this(ofertaId, medioPago, lineas, null, null, null, null, null);
    }

    public record Linea(@NotNull Long varianteId, @Min(1) int cantidad) {}
}
