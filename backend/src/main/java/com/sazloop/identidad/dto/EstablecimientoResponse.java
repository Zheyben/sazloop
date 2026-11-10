package com.sazloop.identidad.dto;

import com.sazloop.identidad.domain.Establecimiento;

import java.math.BigDecimal;

/**
 * Resumen del establecimiento registrado, su estado de afiliación y su cuenta operativa.
 * Incluye las coordenadas si el local ya registró su ubicación (si no, vienen vacías).
 */
public record EstablecimientoResponse(
        Long id, String nombre, String distrito, String direccion,
        Establecimiento.EstadoEstablecimiento estado, String cuentaTrabajador,
        BigDecimal latitud, BigDecimal longitud
) {
}
