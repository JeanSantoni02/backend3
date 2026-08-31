package com.bank.xyz.batch.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cuentas")
public class Cuenta {
    
    @Id
    @Column(name = "cuenta_id")
    private Integer cuentaId;
    
    private String nombre;
    private Double saldo;
    private Integer edad;
    private String tipo;

    // Constructor vacío
    public Cuenta() {}

    // Constructor con todos los campos
    public Cuenta(Integer cuentaId, String nombre, Double saldo, Integer edad, String tipo) {
        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldo = saldo;
        this.edad = edad;
        this.tipo = tipo;
    }

    // Getters y Setters
    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
    
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}