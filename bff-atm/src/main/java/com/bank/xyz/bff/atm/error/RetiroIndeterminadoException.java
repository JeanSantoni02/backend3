package com.bank.xyz.bff.atm.error;

// Se lanza cuando el BFF no pudo confirmar si el debito se aplico o no, por
// ejemplo si el servicio de dominio corto la conexion durante el POST.
//
// Es el peor escenario para un cajero: NO debe entregar el dinero, y la
// operacion debe quedar marcada para conciliacion. Como el retiro es
// idempotente por su referencia, el cajero puede reintentar la misma
// referencia mas tarde sin riesgo de debitar dos veces.
public class RetiroIndeterminadoException extends RuntimeException {

    private final String referencia;

    public RetiroIndeterminadoException(String referencia) {
        super("no se pudo confirmar el resultado del retiro");
        this.referencia = referencia;
    }

    public String getReferencia() {
        return referencia;
    }
}
