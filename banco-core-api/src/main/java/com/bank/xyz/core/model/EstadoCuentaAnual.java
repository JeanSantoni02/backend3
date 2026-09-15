package com.bank.xyz.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "estado_cuenta_anual")
public class EstadoCuentaAnual {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "cuenta_id")
    private Integer cuentaId;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "cantidad_movimientos")
    private Long cantidadMovimientos;

    @Column(name = "total_depositos")
    private BigDecimal totalDepositos;

    @Column(name = "total_retiros")
    private BigDecimal totalRetiros;

    @Column(name = "total_compras")
    private BigDecimal totalCompras;

    @Column(name = "total_pagos")
    private BigDecimal totalPagos;

    @Column(name = "saldo_neto")
    private BigDecimal saldoNeto;

    @Column(name = "movimientos_con_anomalia")
    private Long movimientosConAnomalia;

    public Long getId() { return id; }
    public Integer getCuentaId() { return cuentaId; }
    public Integer getAnio() { return anio; }
    public Long getCantidadMovimientos() { return cantidadMovimientos; }
    public BigDecimal getTotalDepositos() { return totalDepositos; }
    public BigDecimal getTotalRetiros() { return totalRetiros; }
    public BigDecimal getTotalCompras() { return totalCompras; }
    public BigDecimal getTotalPagos() { return totalPagos; }
    public BigDecimal getSaldoNeto() { return saldoNeto; }
    public Long getMovimientosConAnomalia() { return movimientosConAnomalia; }
}
