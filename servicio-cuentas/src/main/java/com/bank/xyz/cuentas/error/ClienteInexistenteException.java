package com.bank.xyz.cuentas.error;

// Respuesta valida del servicio de clientes: no cuenta como falla para el circuito
public class ClienteInexistenteException extends RuntimeException {

    public ClienteInexistenteException(Long clienteId) {
        super("no existe el cliente " + clienteId);
    }
}
