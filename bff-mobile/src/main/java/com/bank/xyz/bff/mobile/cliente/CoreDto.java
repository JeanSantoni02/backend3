package com.bank.xyz.bff.mobile.cliente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

// El movil solo consume tres cosas del servicio de dominio, asi que su reflejo
// del contrato es deliberadamente mas chico que el del BFF web.
public final class CoreDto {

    private CoreDto() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cuenta(Integer cuentaId, String nombre, String tipo, BigDecimal saldoFinal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Saldo(Integer cuentaId, BigDecimal saldo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Movimiento(Long id, LocalDate fecha, String tipo, BigDecimal monto,
                             String descripcion) {
    }
}
