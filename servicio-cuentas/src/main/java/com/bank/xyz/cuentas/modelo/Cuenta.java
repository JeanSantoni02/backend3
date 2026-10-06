package com.bank.xyz.cuentas.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Misma tabla que carga el batch; las abiertas aqui parten en 100000 para no chocar con las legacy
@Entity
@Table(name = "cuentas")
public class Cuenta {

    public static final int PRIMERA_CUENTA_EN_LINEA = 100_000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cuentas_en_linea")
    @SequenceGenerator(name = "cuentas_en_linea", sequenceName = "cuentas_en_linea_seq",
            initialValue = PRIMERA_CUENTA_EN_LINEA, allocationSize = 1)
    @Column(name = "cuenta_id")
    private Integer cuentaId;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "tipo", length = 30)
    private String tipo;

    @Column(name = "saldo_final", precision = 18, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 10)
    private EstadoCuenta estado;

    @Column(name = "abierta_en")
    private LocalDateTime abiertaEn;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public enum EstadoCuenta { ACTIVA, BLOQUEADA, CERRADA }

    // Una cuenta migrada sin estado se considera activa
    public EstadoCuenta estadoEfectivo() {
        return estado == null ? EstadoCuenta.ACTIVA : estado;
    }

    public boolean esMigrada() {
        return cuentaId != null && cuentaId < PRIMERA_CUENTA_EN_LINEA;
    }

    public Integer getCuentaId() { return cuentaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public EstadoCuenta getEstado() { return estado; }
    public void setEstado(EstadoCuenta estado) { this.estado = estado; }
    public LocalDateTime getAbiertaEn() { return abiertaEn; }
    public void setAbiertaEn(LocalDateTime abiertaEn) { this.abiertaEn = abiertaEn; }
    public LocalDateTime getCerradaEn() { return cerradaEn; }
    public void setCerradaEn(LocalDateTime cerradaEn) { this.cerradaEn = cerradaEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
