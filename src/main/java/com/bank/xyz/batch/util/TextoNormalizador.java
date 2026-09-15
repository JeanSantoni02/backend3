package com.bank.xyz.batch.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;


public final class TextoNormalizador {

    /** \p{M} son las marcas combinantes, es decir los acentos ya separados por NFD. */
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");

    private TextoNormalizador() {
    }

    /** Pasa a minusculas, recorta y quita tildes y demas diacriticos. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toLowerCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            return "";
        }
        String descompuesto = Normalizer.normalize(limpio, Normalizer.Form.NFD);
        return DIACRITICOS.matcher(descompuesto).replaceAll("");
    }

    public static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
