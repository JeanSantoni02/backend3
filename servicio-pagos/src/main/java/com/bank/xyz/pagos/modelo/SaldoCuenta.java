package com.bank.xyz.pagos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Vista minima de la cuenta: este servicio solo necesita el saldo y saber si opera
@Entity
@Table(name = "cuentas")
public class SaldoCuenta {

    @Id
    @Column(name = "cuenta_id")
    private Integer cuentaId;

    @Column(name = "saldo_final", precision = 18, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "estado", length = 10)
    private String estado;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // Las cuentas migradas no traen estado y se consideran activas
    public boolean operativa() {
        return estado == null || "ACTIVA".equals(estado);
    }

    public String estadoEfectivo() {
        return estado == null ? "ACTIVA" : estado;
    }

    public BigDecimal saldo() {
        return saldoFinal == null ? BigDecimal.ZERO : saldoFinal;
    }

    public void abonar(BigDecimal monto) {
        saldoFinal = saldo().add(monto);
        actualizadoEn = LocalDateTime.now();
    }

    public void debitar(BigDecimal monto) {
        saldoFinal = saldo().subtract(monto);
        actualizadoEn = LocalDateTime.now();
    }

    public Integer getCuentaId() { return cuentaId; }
}
