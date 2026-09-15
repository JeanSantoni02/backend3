package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "movimientos_anuales", indexes = {
        @Index(name = "idx_mov_cuenta_anio", columnList = "cuenta_id,anio")
})
public class MovimientoAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "fecha_original", length = 30)
    private String fechaOriginal;

    /** Tipo ya normalizado: deposito con y sin tilde colapsan en el mismo valor. */
    @Column(name = "tipo_movimiento", length = 30)
    private String tipoMovimiento;

    @Column(name = "tipo_original", length = 30)
    private String tipoOriginal;

    @Column(name = "monto", precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoRegistro estado = EstadoRegistro.VALIDO;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getFechaOriginal() { return fechaOriginal; }
    public void setFechaOriginal(String fechaOriginal) { this.fechaOriginal = fechaOriginal; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public String getTipoOriginal() { return tipoOriginal; }
    public void setTipoOriginal(String tipoOriginal) { this.tipoOriginal = tipoOriginal; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public EstadoRegistro getEstado() { return estado; }
    public void setEstado(EstadoRegistro estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getProcesadoEn() { return procesadoEn; }
    public void setProcesadoEn(LocalDateTime procesadoEn) { this.procesadoEn = procesadoEn; }
}
