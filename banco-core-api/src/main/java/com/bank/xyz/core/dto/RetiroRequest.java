package com.bank.xyz.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RetiroRequest(
        @NotNull(message = "el monto es obligatorio")
        @DecimalMin(value = "1000", message = "el monto minimo de retiro es 1000")
        @Digits(integer = 12, fraction = 2, message = "monto con formato invalido")
        BigDecimal monto,

        @NotBlank(message = "la referencia de la operacion es obligatoria")
        String referencia) {
}
