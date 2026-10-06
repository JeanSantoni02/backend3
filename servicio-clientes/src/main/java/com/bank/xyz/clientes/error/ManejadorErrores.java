package com.bank.xyz.clientes.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorErrores {

    public record Error(String error, String mensaje, LocalDateTime fecha) {
    }

    public static class NoEncontradoException extends RuntimeException {
        public NoEncontradoException(String mensaje) { super(mensaje); }
    }

    public static class ConflictoException extends RuntimeException {
        public ConflictoException(String mensaje) { super(mensaje); }
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Error> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Error> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> invalido(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", detalle);
    }

    private ResponseEntity<Error> responder(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).body(new Error(codigo, mensaje, LocalDateTime.now()));
    }
}
