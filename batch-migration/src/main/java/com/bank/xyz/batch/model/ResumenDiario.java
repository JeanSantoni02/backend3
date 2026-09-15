package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "resumen_diario",
       uniqueConstraints = @UniqueConstraint(name = "uk_resumen_fecha", columnNames = "fecha"))
public class ResumenDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "total_transacciones", nullable = false)
    private Long totalTransacciones;

    @Column(name = "monto_total", precision = 18, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_promedio", precision = 18, scale = 2)
    private BigDecimal montoPromedio;

    @Column(name = "monto_maximo", precision = 18, scale = 2)
    private BigDecimal montoMaximo;

    @Column(name = "cantidad_anomalias", nullable = false)
    private Long cantidadAnomalias;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Long getTotalTransacciones() { return totalTransacciones; }
    public void setTotalTransacciones(Long totalTransacciones) { this.totalTransacciones = totalTransacciones; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public BigDecimal getMontoPromedio() { return montoPromedio; }
    public void setMontoPromedio(BigDecimal montoPromedio) { this.montoPromedio = montoPromedio; }

    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public void setMontoMaximo(BigDecimal montoMaximo) { this.montoMaximo = montoMaximo; }

    public Long getCantidadAnomalias() { return cantidadAnomalias; }
    public void setCantidadAnomalias(Long cantidadAnomalias) { this.cantidadAnomalias = cantidadAnomalias; }

    public LocalDateTime getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(LocalDateTime generadoEn) { this.generadoEn = generadoEn; }
}
