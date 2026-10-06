package com.bank.xyz.pagos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos", indexes = {
        @Index(name = "ix_pagos_origen", columnList = "cuenta_origen"),
        @Index(name = "ix_pagos_destino", columnList = "cuenta_destino")})
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La referencia la entrega quien origina la operacion y hace idempotente el reintento
    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoPago tipo;

    @Column(name = "cuenta_origen")
    private Integer cuentaOrigen;

    @Column(name = "cuenta_destino")
    private Integer cuentaDestino;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(length = 120)
    private String glosa;

    @Column(name = "saldo_resultante", precision = 18, scale = 2)
    private BigDecimal saldoResultante;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public enum TipoPago { DEPOSITO, TRANSFERENCIA, PAGO }

    protected Pago() {
    }

    public Pago(String referencia, TipoPago tipo, Integer cuentaOrigen, Integer cuentaDestino,
                BigDecimal monto, String glosa, BigDecimal saldoResultante) {
        this.referencia = referencia;
        this.tipo = tipo;
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
        this.glosa = glosa;
        this.saldoResultante = saldoResultante;
    }

    public Long getId() { return id; }
    public String getReferencia() { return referencia; }
    public TipoPago getTipo() { return tipo; }
    public Integer getCuentaOrigen() { return cuentaOrigen; }
    public Integer getCuentaDestino() { return cuentaDestino; }
    public BigDecimal getMonto() { return monto; }
    public String getGlosa() { return glosa; }
    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
