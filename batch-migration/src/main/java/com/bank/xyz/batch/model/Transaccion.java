package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "transacciones", indexes = {
        @Index(name = "idx_transacciones_fecha", columnList = "fecha"),
        @Index(name = "idx_transacciones_estado", columnList = "estado")
})
public class Transaccion {

    @Id
    @Column(name = "id")
    private Long id;

    /** Fecha ya normalizada. Null cuando el archivo traia una fecha imposible. */
    @Column(name = "fecha")
    private LocalDate fecha;

    /** Texto original, para poder auditar que venia en el legacy. */
    @Column(name = "fecha_original", length = 30)
    private String fechaOriginal;

    @Column(name = "monto", precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "tipo", length = 30)
    private String tipo;

    @Column(name = "tipo_original", length = 30)
    private String tipoOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoRegistro estado = EstadoRegistro.VALIDO;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getFechaOriginal() { return fechaOriginal; }
    public void setFechaOriginal(String fechaOriginal) { this.fechaOriginal = fechaOriginal; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTipoOriginal() { return tipoOriginal; }
    public void setTipoOriginal(String tipoOriginal) { this.tipoOriginal = tipoOriginal; }

    public EstadoRegistro getEstado() { return estado; }
    public void setEstado(EstadoRegistro estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getProcesadoEn() { return procesadoEn; }
    public void setProcesadoEn(LocalDateTime procesadoEn) { this.procesadoEn = procesadoEn; }
}
