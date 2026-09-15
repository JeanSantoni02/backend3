package com.bank.xyz.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "resumen_diario")
public class ResumenDiario {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "total_transacciones")
    private Long totalTransacciones;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Column(name = "monto_promedio")
    private BigDecimal montoPromedio;

    @Column(name = "monto_maximo")
    private BigDecimal montoMaximo;

    @Column(name = "cantidad_anomalias")
    private Long cantidadAnomalias;

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public Long getTotalTransacciones() { return totalTransacciones; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public BigDecimal getMontoPromedio() { return montoPromedio; }
    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public Long getCantidadAnomalias() { return cantidadAnomalias; }
}
