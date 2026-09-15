package com.bank.xyz.bff.mobile.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    // Error compacto: dos campos. La app solo necesita un codigo para decidir
    // que mensaje mostrar, no un objeto con ruta, timestamp y detalles.
    public record ErrorMovil(String codigo, String mensaje) {
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorMovil> noEncontrado(RecursoNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMovil("NO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(CoreApiNoDisponibleException.class)
    public ResponseEntity<ErrorMovil> sinBackend(CoreApiNoDisponibleException e) {
        log.error("Servicio de dominio no disponible: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorMovil("SIN_SERVICIO", "intenta nuevamente en unos momentos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMovil> inesperado(Exception e, HttpServletRequest p) {
        log.error("Error no controlado en {}", p.getRequestURI(), e);
        return ResponseEntity.internalServerError()
                .body(new ErrorMovil("ERROR", "no pudimos completar la operacion"));
    }
}
