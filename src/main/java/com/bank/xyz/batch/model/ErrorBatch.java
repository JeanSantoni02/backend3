package com.bank.xyz.batch.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "errores_batch", indexes = {
        @Index(name = "idx_errores_job", columnList = "job_nombre")
})
public class ErrorBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_nombre", length = 60)
    private String jobNombre;

    @Column(name = "step_nombre", length = 80)
    private String stepNombre;

    /** LECTURA, PROCESO o ESCRITURA. */
    @Column(name = "fase", length = 15)
    private String fase;

    @Column(name = "motivo", length = 300)
    private String motivo;

    @Column(name = "registro", length = 500)
    private String registro;

    @Column(name = "excepcion", length = 200)
    private String excepcion;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn = LocalDateTime.now();

    public ErrorBatch() {
    }

    public ErrorBatch(String jobNombre, String stepNombre, String fase,
                      String motivo, String registro, String excepcion) {
        this.jobNombre = jobNombre;
        this.stepNombre = stepNombre;
        this.fase = fase;
        this.motivo = motivo;
        this.registro = registro;
        this.excepcion = excepcion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobNombre() { return jobNombre; }
    public void setJobNombre(String jobNombre) { this.jobNombre = jobNombre; }

    public String getStepNombre() { return stepNombre; }
    public void setStepNombre(String stepNombre) { this.stepNombre = stepNombre; }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getRegistro() { return registro; }
    public void setRegistro(String registro) { this.registro = registro; }

    public String getExcepcion() { return excepcion; }
    public void setExcepcion(String excepcion) { this.excepcion = excepcion; }

    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }
}
