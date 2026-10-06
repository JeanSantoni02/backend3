package com.bank.xyz.cuentas.evento;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventoTransaccion implements Serializable {

    private String eventoId;
    private String tipo;
    private Integer cuentaId;
    private BigDecimal monto;
    private BigDecimal saldoResultante;
    private String referencia;
    private String canal;
    private LocalDateTime ocurridoEn;

    public EventoTransaccion() {
    }

    public EventoTransaccion(String eventoId, String tipo, Integer cuentaId, BigDecimal monto,
                             BigDecimal saldoResultante, String referencia, String canal,
                             LocalDateTime ocurridoEn) {
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.cuentaId = cuentaId;
        this.monto = monto;
        this.saldoResultante = saldoResultante;
        this.referencia = referencia;
        this.canal = canal;
        this.ocurridoEn = ocurridoEn;
    }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public void setSaldoResultante(BigDecimal saldoResultante) { this.saldoResultante = saldoResultante; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }
}
