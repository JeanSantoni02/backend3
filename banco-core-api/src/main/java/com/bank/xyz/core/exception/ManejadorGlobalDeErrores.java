package com.bank.xyz.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e,
                                                      HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.de(404, "RECURSO_NO_ENCONTRADO", e.getMessage(), peticion.getRequestURI()));
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErrorResponse> saldoInsuficiente(SaldoInsuficienteException e,
                                                           HttpServletRequest peticion) {
        return ResponseEntity.unprocessableEntity().body(new ErrorResponse(
                422, "SALDO_INSUFICIENTE", e.getMessage(),
                List.of("saldo disponible: " + e.getSaldoDisponible()),
                peticion.getRequestURI(), LocalDateTime.now()));
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> operacionInvalida(OperacionInvalidaException e,
                                                           HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.de(400, "OPERACION_INVALIDA", e.getMessage(), peticion.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e,
                                                    HttpServletRequest peticion) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "DATOS_INVALIDOS", "la peticion no cumple las validaciones",
                detalles, peticion.getRequestURI(), LocalDateTime.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e,
                                                        HttpServletRequest peticion) {
        log.warn("Peticion con cuerpo JSON invalido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.de(
                400, "JSON_INVALIDO", "el cuerpo de la peticion no es JSON valido",
                peticion.getRequestURI()));
    }

    // Nunca devolver el stacktrace ni el mensaje interno al cliente:
    // puede filtrar nombres de tablas y detalles de la infraestructura.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInesperado(Exception e, HttpServletRequest peticion) {
        log.error("Error no controlado en {}", peticion.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(
                ErrorResponse.de(500, "ERROR_INTERNO",
                        "ocurrio un error procesando la solicitud", peticion.getRequestURI()));
    }
}
