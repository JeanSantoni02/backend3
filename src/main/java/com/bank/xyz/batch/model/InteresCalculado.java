package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "intereses_calculados", indexes = {
        @Index(name = "idx_intereses_cuenta", columnList = "cuenta_id")
})
public class InteresCalculado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "tipo_cuenta", length = 30)
    private String tipoCuenta;

    @Column(name = "edad")
    private Integer edad;

    @Column(name = "saldo_inicial", precision = 18, scale = 2)
    private BigDecimal saldoInicial;

    /** Tasa anual aplicada segun el tipo de cuenta, en porcentaje. */
    @Column(name = "tasa_anual", precision = 6, scale = 3)
    private BigDecimal tasaAnual;

    @Column(name = "interes_mensual", precision = 18, scale = 2)
    private BigDecimal interesMensual;

    /** saldo_inicial + interes_mensual (en prestamo e hipoteca el interes aumenta la deuda). */
    @Column(name = "saldo_final", precision = 18, scale = 2)
    private BigDecimal saldoFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoRegistro estado = EstadoRegistro.VALIDO;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "calculado_en", nullable = false)
    private LocalDateTime calculadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }

    public BigDecimal getTasaAnual() { return tasaAnual; }
    public void setTasaAnual(BigDecimal tasaAnual) { this.tasaAnual = tasaAnual; }

    public BigDecimal getInteresMensual() { return interesMensual; }
    public void setInteresMensual(BigDecimal interesMensual) { this.interesMensual = interesMensual; }

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public EstadoRegistro getEstado() { return estado; }
    public void setEstado(EstadoRegistro estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getCalculadoEn() { return calculadoEn; }
    public void setCalculadoEn(LocalDateTime calculadoEn) { this.calculadoEn = calculadoEn; }
}
