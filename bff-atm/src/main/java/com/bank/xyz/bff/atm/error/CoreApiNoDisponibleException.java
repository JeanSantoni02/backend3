package com.bank.xyz.bff.atm.error;

public class CoreApiNoDisponibleException extends RuntimeException {
    public CoreApiNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
