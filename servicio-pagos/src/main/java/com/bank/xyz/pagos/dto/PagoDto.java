package com.bank.xyz.pagos.dto;

import com.bank.xyz.pagos.modelo.Pago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PagoDto {

    private PagoDto() {
    }

    public record Deposito(
            @NotNull Integer cuentaId,
            @NotNull @DecimalMin(value = "1", message = "el monto debe ser positivo")
            @Digits(integer = 16, fraction = 2) BigDecimal monto,
            @NotBlank @Size(max = 60) String referencia,
            @Size(max = 120) String glosa) {
    }

    public record Transferencia(
            @NotNull Integer cuentaOrigen,
            @NotNull Integer cuentaDestino,
            @NotNull @DecimalMin(value = "1", message = "el monto debe ser positivo")
            @Digits(integer = 16, fraction = 2) BigDecimal monto,
            @NotBlank @Size(max = 60) String referencia,
            @Size(max = 120) String glosa) {
    }

    // Pago de un servicio o convenio: debita la cuenta del cliente
    public record PagoServicio(
            @NotNull Integer cuentaId,
            @NotNull @DecimalMin(value = "1", message = "el monto debe ser positivo")
            @Digits(integer = 16, fraction = 2) BigDecimal monto,
            @NotBlank @Size(max = 60) String referencia,
            @NotBlank @Size(max = 120) String convenio) {
    }

    public record Respuesta(
            String referencia, Pago.TipoPago tipo, Integer cuentaOrigen, Integer cuentaDestino,
            BigDecimal monto, String glosa, BigDecimal saldoResultante, boolean duplicado,
            LocalDateTime fecha) {

        public static Respuesta de(Pago p, boolean duplicado) {
            return new Respuesta(p.getReferencia(), p.getTipo(), p.getCuentaOrigen(),
                    p.getCuentaDestino(), p.getMonto(), p.getGlosa(), p.getSaldoResultante(),
                    duplicado, p.getCreadoEn());
        }
    }
}
