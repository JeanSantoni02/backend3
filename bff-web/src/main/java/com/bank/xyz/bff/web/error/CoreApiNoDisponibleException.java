package com.bank.xyz.bff.web.error;

public class CoreApiNoDisponibleException extends RuntimeException {
    public CoreApiNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
