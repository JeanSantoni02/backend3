package com.bank.xyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenDiarioDto(
        LocalDate fecha,
        Long totalTransacciones,
        BigDecimal montoTotal,
        BigDecimal montoPromedio,
        BigDecimal montoMaximo,
        Long cantidadAnomalias) {
}
