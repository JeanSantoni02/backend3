package com.bank.xyz.cuentas.error;

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

    public static class OperacionInvalidaException extends RuntimeException {
        private final String codigo;
        public OperacionInvalidaException(String codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }
        public String getCodigo() { return codigo; }
    }

    public static class NoEncontradaException extends RuntimeException {
        public NoEncontradaException(String mensaje) { super(mensaje); }
    }

    public static class ServicioNoDisponibleException extends RuntimeException {
        public ServicioNoDisponibleException(String mensaje) { super(mensaje); }
    }

    @ExceptionHandler(NoEncontradaException.class)
    public ResponseEntity<Error> noEncontrada(NoEncontradaException e) {
        return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADA", e.getMessage());
    }

    @ExceptionHandler(ClienteInexistenteException.class)
    public ResponseEntity<Error> clienteInexistente(ClienteInexistenteException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "CLIENTE_INEXISTENTE", e.getMessage());
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<Error> invalida(OperacionInvalidaException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Error> noDisponible(ServicioNoDisponibleException e) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> datos(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", detalle);
    }

    private ResponseEntity<Error> responder(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).body(new Error(codigo, mensaje, LocalDateTime.now()));
    }
}
