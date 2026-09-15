package com.bank.xyz.bff.mobile.error;

public class CoreApiNoDisponibleException extends RuntimeException {
    public CoreApiNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
