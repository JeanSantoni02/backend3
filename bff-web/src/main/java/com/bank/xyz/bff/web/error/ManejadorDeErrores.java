package com.bank.xyz.bff.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    public record ErrorWeb(int estado, String error, String mensaje, String ruta, LocalDateTime momento) {
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorWeb> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest p) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorWeb(
                404, "RECURSO_NO_ENCONTRADO", e.getMessage(), p.getRequestURI(), LocalDateTime.now()));
    }

    @ExceptionHandler(CoreApiNoDisponibleException.class)
    public ResponseEntity<ErrorWeb> sinBackend(CoreApiNoDisponibleException e, HttpServletRequest p) {
        log.error("Servicio de dominio no disponible: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorWeb(
                503, "SERVICIO_NO_DISPONIBLE",
                "el portal no puede obtener los datos en este momento",
                p.getRequestURI(), LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorWeb> inesperado(Exception e, HttpServletRequest p) {
        log.error("Error no controlado en {}", p.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(new ErrorWeb(
                500, "ERROR_INTERNO", "ocurrio un error procesando la solicitud",
                p.getRequestURI(), LocalDateTime.now()));
    }
}
