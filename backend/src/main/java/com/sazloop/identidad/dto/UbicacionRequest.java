package com.sazloop.identidad.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Ubicación (latitud y longitud) de un local, tomada con el GPS del navegador del dueño. */
public record UbicacionRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud
) {
}
