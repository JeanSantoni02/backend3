package com.bank.xyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaldoDto(Integer cuentaId, BigDecimal saldo, LocalDateTime consultadoEn) {
}
