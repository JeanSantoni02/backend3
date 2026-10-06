package com.bank.xyz.cuentas.dto;

import com.bank.xyz.cuentas.modelo.Cuenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CuentaDto {

    public static final String TIPOS = "ahorro|corriente|prestamo|hipoteca";

    private CuentaDto() {
    }

    public record Apertura(
            @NotNull Long clienteId,
            @NotNull @Pattern(regexp = TIPOS, message = "tipo debe ser " + TIPOS) String tipo,
            @DecimalMin(value = "0", message = "el deposito inicial no puede ser negativo")
            @Digits(integer = 16, fraction = 2) BigDecimal depositoInicial) {
    }

    // Mantenimiento: solo lo que puede cambiar en una cuenta viva
    public record Mantenimiento(
            @Pattern(regexp = TIPOS, message = "tipo debe ser " + TIPOS) String tipo,
            @Pattern(regexp = "ACTIVA|BLOQUEADA", message = "estado debe ser ACTIVA o BLOQUEADA") String estado) {
    }

    public record Respuesta(
            Integer cuentaId, Long clienteId, String titular, String tipo, BigDecimal saldo,
            String estado, boolean migrada, LocalDateTime abiertaEn, LocalDateTime cerradaEn) {

        public static Respuesta de(Cuenta c) {
            return new Respuesta(c.getCuentaId(), c.getClienteId(), c.getNombre(), c.getTipo(),
                    c.getSaldoFinal(), c.estadoEfectivo().name(), c.esMigrada(),
                    c.getAbiertaEn(), c.getCerradaEn());
        }
    }
}
