package com.bank.xyz.batch.exception;


public class RegistroInvalidoException extends RuntimeException {

    private final String motivo;
    private final String datoOriginal;

    public RegistroInvalidoException(String motivo, String datoOriginal) {
        super(motivo + " | dato: " + datoOriginal);
        this.motivo = motivo;
        this.datoOriginal = datoOriginal;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDatoOriginal() {
        return datoOriginal;
    }
}
