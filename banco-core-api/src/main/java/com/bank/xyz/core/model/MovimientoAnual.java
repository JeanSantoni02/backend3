package com.bank.xyz.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_anuales")
public class MovimientoAnual {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "cuenta_id")
    private Integer cuentaId;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "tipo_movimiento")
    private String tipoMovimiento;

    @Column(name = "monto")
    private BigDecimal monto;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estado")
    private String estado;

    @Column(name = "observaciones")
    private String observaciones;

    public Long getId() { return id; }
    public Integer getCuentaId() { return cuentaId; }
    public Integer getAnio() { return anio; }
    public LocalDate getFecha() { return fecha; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public BigDecimal getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }
}
