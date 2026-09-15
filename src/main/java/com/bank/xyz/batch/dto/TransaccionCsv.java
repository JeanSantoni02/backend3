package com.bank.xyz.batch.dto;


public class TransaccionCsv {

    private String id;
    private String fecha;
    private String monto;
    private String tipo;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return "id=" + id + ", fecha=" + fecha + ", monto=" + monto + ", tipo=" + tipo;
    }
}
