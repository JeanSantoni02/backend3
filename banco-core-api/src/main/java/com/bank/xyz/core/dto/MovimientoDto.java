package com.bank.xyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoDto(
        Long id,
        Integer cuentaId,
        Integer anio,
        LocalDate fecha,
        String tipo,
        BigDecimal monto,
        String descripcion,
        String estado,
        String observaciones) {
}
