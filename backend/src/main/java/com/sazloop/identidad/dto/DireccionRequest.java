package com.sazloop.identidad.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Ubicación textual de un establecimiento (RF-05), con coordenadas opcionales para «Cerca de mí».
 * La latitud va de -90 a 90 y la longitud de -180 a 180.
 */
public record DireccionRequest(
        @NotBlank @Size(max = 80) String departamento,
        @NotBlank @Size(max = 80) String provincia,
        @NotBlank @Size(max = 80) String distrito,
        @NotBlank @Size(max = 180) String direccionLinea,
        @Size(max = 20) String numero,
        @Size(max = 250) String referencia,
        @Size(max = 10) String codigoPostal,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud
) {
    /** Dirección sin coordenadas (la forma que se usaba antes de agregar «Cerca de mí»). */
    public DireccionRequest(String departamento, String provincia, String distrito, String direccionLinea,
                            String numero, String referencia, String codigoPostal) {
        this(departamento, provincia, distrito, direccionLinea, numero, referencia, codigoPostal, null, null);
    }
}
