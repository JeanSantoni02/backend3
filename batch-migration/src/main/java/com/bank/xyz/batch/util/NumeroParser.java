package com.bank.xyz.batch.util;

import java.math.BigDecimal;


public final class NumeroParser {

    private NumeroParser() {
    }

    /** @return el importe, o null si el campo viene vacio o no es numerico. */
    public static BigDecimal aImporte(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return el entero, o null si el campo viene vacio o no es numerico. */
    public static Integer aEntero(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
