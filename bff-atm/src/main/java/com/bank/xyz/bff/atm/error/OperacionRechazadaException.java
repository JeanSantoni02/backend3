package com.bank.xyz.bff.atm.error;

import org.springframework.http.HttpStatus;

public class OperacionRechazadaException extends RuntimeException {

    private final String codigo;
    private final HttpStatus estado;

    public OperacionRechazadaException(String codigo, String mensaje) {
        this(codigo, mensaje, HttpStatus.BAD_REQUEST);
    }

    // Se distingue el estado HTTP porque no es lo mismo una peticion mal
    // formada (400) que una peticion correcta que el negocio no puede cumplir
    // (422, saldo insuficiente). El cajero usa esa diferencia para decidir si
    // pide otro monto o cancela la operacion.
    public OperacionRechazadaException(String codigo, String mensaje, HttpStatus estado) {
        super(mensaje);
        this.codigo = codigo;
        this.estado = estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
