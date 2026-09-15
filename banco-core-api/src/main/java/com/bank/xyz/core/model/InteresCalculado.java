package com.bank.xyz.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "intereses_calculados")
public class InteresCalculado {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "cuenta_id")
    private Integer cuentaId;

    @Column(name = "tipo_cuenta")
    private String tipoCuenta;

    @Column(name = "saldo_inicial")
    private BigDecimal saldoInicial;

    @Column(name = "tasa_anual")
    private BigDecimal tasaAnual;

    @Column(name = "interes_mensual")
    private BigDecimal interesMensual;

    @Column(name = "saldo_final")
    private BigDecimal saldoFinal;

    @Column(name = "estado")
    private String estado;

    public Long getId() { return id; }
    public Integer getCuentaId() { return cuentaId; }
    public String getTipoCuenta() { return tipoCuenta; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public BigDecimal getTasaAnual() { return tasaAnual; }
    public BigDecimal getInteresMensual() { return interesMensual; }
    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public String getEstado() { return estado; }
}
