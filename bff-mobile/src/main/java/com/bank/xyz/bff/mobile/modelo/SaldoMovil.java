package com.bank.xyz.bff.mobile.modelo;

import java.math.BigDecimal;

// La consulta de saldo es la operacion mas frecuente en movil: se deja en
// dos campos y nada mas.
public record SaldoMovil(Integer id, BigDecimal saldo) {
}
