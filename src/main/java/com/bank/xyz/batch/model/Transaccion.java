package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "transacciones")
public class Transaccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "numero_transaccion")
    private Integer numeroTransaccion;
    
    private LocalDate fecha;
    private Double monto;
    private String tipo;
    
    @Column(name = "es_anomalia")
    private Boolean esAnomalia = false;
    
    @Column(name = "comentario_error", length = 500)
    private String comentarioError;

    // Constructor vacío
    public Transaccion() {}

    // Constructor con todos los campos
    public Transaccion(Long id, Integer numeroTransaccion, LocalDate fecha, 
                       Double monto, String tipo, Boolean esAnomalia, String comentarioError) {
        this.id = id;
        this.numeroTransaccion = numeroTransaccion;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.esAnomalia = esAnomalia;
        this.comentarioError = comentarioError;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getNumeroTransaccion() { return numeroTransaccion; }
    public void setNumeroTransaccion(Integer numeroTransaccion) { this.numeroTransaccion = numeroTransaccion; }
    
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public Boolean getEsAnomalia() { return esAnomalia; }
    public void setEsAnomalia(Boolean esAnomalia) { this.esAnomalia = esAnomalia; }
    
    public String getComentarioError() { return comentarioError; }
    public void setComentarioError(String comentarioError) { this.comentarioError = comentarioError; }
}
