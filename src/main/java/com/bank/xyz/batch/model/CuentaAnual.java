package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cuentas_anuales")
public class CuentaAnual {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cuenta_id")
    private Integer cuentaId;
    
    private LocalDate fecha;
    private String transaccion;
    private Double monto;
    private String descripcion;

    // Constructor vacío
    public CuentaAnual() {}

    // Constructor con todos los campos
    public CuentaAnual(Long id, Integer cuentaId, LocalDate fecha, 
                       String transaccion, Double monto, String descripcion) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }
    
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    
    public String getTransaccion() { return transaccion; }
    public void setTransaccion(String transaccion) { this.transaccion = transaccion; }
    
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}