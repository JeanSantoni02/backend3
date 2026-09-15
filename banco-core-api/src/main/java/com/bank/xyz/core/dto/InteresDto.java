package com.bank.xyz.core.dto;

import java.math.BigDecimal;

public record InteresDto(
        Long id,
        Integer cuentaId,
        String tipoCuenta,
        BigDecimal saldoInicial,
        BigDecimal tasaAnual,
        BigDecimal interesMensual,
        BigDecimal saldoFinal,
        String estado) {
}
