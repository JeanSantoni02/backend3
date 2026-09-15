package com.bank.xyz.core.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    private final BigDecimal saldoDisponible;

    public SaldoInsuficienteException(BigDecimal saldoDisponible) {
        super("saldo insuficiente para el retiro solicitado");
        this.saldoDisponible = saldoDisponible;
    }

    public BigDecimal getSaldoDisponible() {
        return saldoDisponible;
    }
}
