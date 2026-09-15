package com.bank.xyz.batch.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;


public final class FechaParser {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            formato("uuuu-MM-dd"),
            formato("uuuu/MM/dd"),
            formato("dd-MM-uuuu"),
            formato("dd/MM/uuuu")
    );

    private FechaParser() {
    }

    private static DateTimeFormatter formato(String patron) {
        return DateTimeFormatter.ofPattern(patron).withResolverStyle(ResolverStyle.STRICT);
    }

    /**
     * @return la fecha, o null si el texto viene vacio o no corresponde a
     *         ninguno de los formatos conocidos (fecha imposible incluida).
     */
    public static LocalDate parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(limpio, formato);
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        return null;
    }
}
