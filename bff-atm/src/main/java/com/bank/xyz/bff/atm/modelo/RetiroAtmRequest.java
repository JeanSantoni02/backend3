package com.bank.xyz.bff.atm.modelo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RetiroAtmRequest(
        @NotNull(message = "el monto es obligatorio")
        @Positive(message = "el monto debe ser mayor a cero")
        BigDecimal monto,

        // Opcional: si el cajero no la envia, el BFF genera una. Enviarla permite
        // reintentar con seguridad tras un corte de red.
        String referencia) {
}
