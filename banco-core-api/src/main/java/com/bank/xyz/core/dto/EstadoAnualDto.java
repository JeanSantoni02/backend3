package com.bank.xyz.core.dto;

import java.math.BigDecimal;

public record EstadoAnualDto(
        Integer cuentaId,
        Integer anio,
        Long cantidadMovimientos,
        BigDecimal totalDepositos,
        BigDecimal totalRetiros,
        BigDecimal totalCompras,
        BigDecimal totalPagos,
        BigDecimal saldoNeto,
        Long movimientosConAnomalia) {
}
