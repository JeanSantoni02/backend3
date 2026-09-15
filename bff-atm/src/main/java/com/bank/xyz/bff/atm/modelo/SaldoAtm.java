package com.bank.xyz.bff.atm.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// La pantalla del cajero muestra el saldo y cuanto puede retirar ahora, que es
// el minimo entre el saldo y el tope por operacion. Calcularlo aqui evita que
// el cliente proponga un monto que el servicio va a rechazar igual.
public record SaldoAtm(Integer cuenta,
                       BigDecimal saldoDisponible,
                       BigDecimal maximoRetirable,
                       BigDecimal denominacion,
                       LocalDateTime consultadoEn) {
}
