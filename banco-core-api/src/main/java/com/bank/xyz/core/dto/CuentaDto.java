package com.bank.xyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaDto(
        Integer cuentaId,
        String nombre,
        String tipo,
        BigDecimal saldoFinal,
        BigDecimal interesTotal,
        Long registrosProcesados,
        LocalDateTime actualizadoEn) {
}
