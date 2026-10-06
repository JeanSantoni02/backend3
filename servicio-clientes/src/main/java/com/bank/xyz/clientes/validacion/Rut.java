package com.bank.xyz.clientes.validacion;

// RUT chileno: el digito verificador se calcula por modulo 11
public final class Rut {

    private Rut() {
    }

    public static String normalizar(String rut) {
        return rut.replace(".", "").replace(" ", "").toUpperCase();
    }

    public static boolean esValido(String rut) {
        String limpio = normalizar(rut);
        if (!limpio.matches("\\d{7,8}-[\\dK]")) {
            return false;
        }

        String cuerpo = limpio.substring(0, limpio.indexOf('-'));
        char dv = limpio.charAt(limpio.length() - 1);

        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }

        int resto = 11 - (suma % 11);
        char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : Character.forDigit(resto, 10);
        return dv == esperado;
    }
}
