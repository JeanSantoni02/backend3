package com.bank.xyz.bff.atm.cliente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CoreDto {

    private CoreDto() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Saldo(Integer cuentaId, BigDecimal saldo, LocalDateTime consultadoEn) {
    }

    public record RetiroRequest(BigDecimal monto, String referencia) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RetiroResponse(Long operacionId, Integer cuentaId, BigDecimal montoRetirado,
                                 BigDecimal saldoAnterior, BigDecimal saldoResultante,
                                 String referencia, boolean reintentoIdempotente,
                                 LocalDateTime ocurridoEn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorCore(int estado, String error, String mensaje) {
    }
}
