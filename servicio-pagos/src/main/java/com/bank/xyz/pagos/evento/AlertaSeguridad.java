package com.bank.xyz.pagos.evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlertaSeguridad {

    private String alertaId;
    private String tipo;
    private String severidad;
    private Integer cuentaId;
    private BigDecimal monto;
    private String detalle;
    private LocalDateTime detectadaEn;

    public AlertaSeguridad() {
    }

    public AlertaSeguridad(String alertaId, String tipo, String severidad, Integer cuentaId,
                           BigDecimal monto, String detalle) {
        this.alertaId = alertaId;
        this.tipo = tipo;
        this.severidad = severidad;
        this.cuentaId = cuentaId;
        this.monto = monto;
        this.detalle = detalle;
        this.detectadaEn = LocalDateTime.now();
    }

    public String getAlertaId() { return alertaId; }
    public void setAlertaId(String alertaId) { this.alertaId = alertaId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public LocalDateTime getDetectadaEn() { return detectadaEn; }
    public void setDetectadaEn(LocalDateTime detectadaEn) { this.detectadaEn = detectadaEn; }
}
