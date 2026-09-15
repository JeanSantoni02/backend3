package com.bank.xyz.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operaciones_atm",
       uniqueConstraints = @UniqueConstraint(name = "uk_operacion_referencia", columnNames = "referencia"),
       indexes = @Index(name = "idx_operacion_cuenta", columnList = "cuenta_id"))
public class OperacionAtm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo;

    @Column(name = "monto", precision = 18, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", precision = 18, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_resultante", precision = 18, scale = 2)
    private BigDecimal saldoResultante;

    // Clave de idempotencia: impide que un reintento del cajero cobre dos veces.
    @Column(name = "referencia", length = 80, nullable = false)
    private String referencia;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn = LocalDateTime.now();

    public Long getId() { return id; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(BigDecimal saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public void setSaldoResultante(BigDecimal saldoResultante) { this.saldoResultante = saldoResultante; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }
}
