package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "estado_cuenta_anual",
       uniqueConstraints = @UniqueConstraint(name = "uk_estado_cuenta_anio",
                                             columnNames = {"cuenta_id", "anio"}))
public class EstadoCuentaAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "cantidad_movimientos", nullable = false)
    private Long cantidadMovimientos;

    @Column(name = "total_depositos", precision = 18, scale = 2)
    private BigDecimal totalDepositos;

    @Column(name = "total_retiros", precision = 18, scale = 2)
    private BigDecimal totalRetiros;

    @Column(name = "total_compras", precision = 18, scale = 2)
    private BigDecimal totalCompras;

    @Column(name = "total_pagos", precision = 18, scale = 2)
    private BigDecimal totalPagos;

    /** depositos menos la suma de retiros, compras y pagos. */
    @Column(name = "saldo_neto", precision = 18, scale = 2)
    private BigDecimal saldoNeto;

    /** Cuantos movimientos del anio quedaron marcados como ANOMALIA. */
    @Column(name = "movimientos_con_anomalia", nullable = false)
    private Long movimientosConAnomalia;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public Long getCantidadMovimientos() { return cantidadMovimientos; }
    public void setCantidadMovimientos(Long cantidadMovimientos) { this.cantidadMovimientos = cantidadMovimientos; }

    public BigDecimal getTotalDepositos() { return totalDepositos; }
    public void setTotalDepositos(BigDecimal totalDepositos) { this.totalDepositos = totalDepositos; }

    public BigDecimal getTotalRetiros() { return totalRetiros; }
    public void setTotalRetiros(BigDecimal totalRetiros) { this.totalRetiros = totalRetiros; }

    public BigDecimal getTotalCompras() { return totalCompras; }
    public void setTotalCompras(BigDecimal totalCompras) { this.totalCompras = totalCompras; }

    public BigDecimal getTotalPagos() { return totalPagos; }
    public void setTotalPagos(BigDecimal totalPagos) { this.totalPagos = totalPagos; }

    public BigDecimal getSaldoNeto() { return saldoNeto; }
    public void setSaldoNeto(BigDecimal saldoNeto) { this.saldoNeto = saldoNeto; }

    public Long getMovimientosConAnomalia() { return movimientosConAnomalia; }
    public void setMovimientosConAnomalia(Long movimientosConAnomalia) { this.movimientosConAnomalia = movimientosConAnomalia; }

    public LocalDateTime getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(LocalDateTime generadoEn) { this.generadoEn = generadoEn; }
}
