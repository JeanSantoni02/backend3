package com.bank.xyz.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @Column(name = "cuenta_id")
    private Integer cuentaId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "registros_procesados")
    private Long registrosProcesados;

    @Column(name = "interes_total")
    private BigDecimal interesTotal;

    @Column(name = "saldo_final")
    private BigDecimal saldoFinal;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Long getRegistrosProcesados() { return registrosProcesados; }
    public void setRegistrosProcesados(Long registrosProcesados) { this.registrosProcesados = registrosProcesados; }

    public BigDecimal getInteresTotal() { return interesTotal; }
    public void setInteresTotal(BigDecimal interesTotal) { this.interesTotal = interesTotal; }

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
