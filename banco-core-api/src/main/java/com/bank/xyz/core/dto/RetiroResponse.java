package com.bank.xyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RetiroResponse(
        Long operacionId,
        Integer cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoAnterior,
        BigDecimal saldoResultante,
        String referencia,
        boolean reintentoIdempotente,
        LocalDateTime ocurridoEn) {
}
