package com.bank.xyz.bff.atm.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA_ATM");

    // El cajero necesita un codigo para decidir que hacer con el efectivo:
    // entregarlo, retenerlo o pedir conciliacion. Por eso "entregarEfectivo" es
    // un campo explicito y no algo que la pantalla deba deducir del mensaje.
    public record ErrorAtm(String codigo, String mensaje, boolean entregarEfectivo,
                           String referencia) {

        static ErrorAtm de(String codigo, String mensaje) {
            return new ErrorAtm(codigo, mensaje, false, null);
        }
    }

    @ExceptionHandler(OperacionRechazadaException.class)
    public ResponseEntity<ErrorAtm> rechazada(OperacionRechazadaException e) {
        return ResponseEntity.status(e.getEstado())
                .body(ErrorAtm.de(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorAtm> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(campo -> campo.getDefaultMessage())
                .orElse("datos invalidos");
        return ResponseEntity.badRequest().body(ErrorAtm.de("DATOS_INVALIDOS", detalle));
    }

    // Un cuerpo JSON malformado es culpa del cliente, no del servidor: sin este
    // manejador cae en el catch-all y se reporta como 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorAtm> cuerpoIlegible(HttpMessageNotReadableException e) {
        log.warn("Peticion con cuerpo JSON invalido: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorAtm.de("JSON_INVALIDO", "el cuerpo de la peticion no es JSON valido"));
    }

    @ExceptionHandler(RetiroIndeterminadoException.class)
    public ResponseEntity<ErrorAtm> indeterminado(RetiroIndeterminadoException e) {
        // 409: el estado real de la operacion es desconocido. Se devuelve la
        // referencia para que el cajero pueda reintentarla o enviarla a
        // conciliacion, y se marca explicitamente que NO entregue efectivo.
        auditoria.error("RETIRO_INDETERMINADO referencia={} requiere conciliacion", e.getReferencia());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorAtm(
                "RETIRO_INDETERMINADO",
                "no se pudo confirmar la operacion, no entregue efectivo",
                false, e.getReferencia()));
    }

    @ExceptionHandler(CoreApiNoDisponibleException.class)
    public ResponseEntity<ErrorAtm> sinBackend(CoreApiNoDisponibleException e) {
        log.error("Servicio de dominio no disponible: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorAtm.de("SIN_SERVICIO", "servicio no disponible, intente mas tarde"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorAtm> inesperado(Exception e, HttpServletRequest p) {
        log.error("Error no controlado en {}", p.getRequestURI(), e);
        return ResponseEntity.internalServerError()
                .body(ErrorAtm.de("ERROR", "no se pudo completar la operacion"));
    }
}
