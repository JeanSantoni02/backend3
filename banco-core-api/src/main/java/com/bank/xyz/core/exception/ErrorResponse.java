package com.bank.xyz.core.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int estado,
        String error,
        String mensaje,
        List<String> detalles,
        String ruta,
        LocalDateTime momento) {

    public static ErrorResponse de(int estado, String error, String mensaje, String ruta) {
        return new ErrorResponse(estado, error, mensaje, List.of(), ruta, LocalDateTime.now());
    }
}
