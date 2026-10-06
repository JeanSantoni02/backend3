package com.bank.xyz.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

// Con el circuito abierto el cliente recibe un 503 claro en vez de esperar el timeout
@RestController
public class RespaldoController {

    private static final Map<String, String> NOMBRES = Map.of(
            "clientes", "gestion de clientes",
            "cuentas", "gestion de cuentas",
            "pagos", "procesamiento de pagos",
            "bff", "canal");

    @RequestMapping("/respaldo/{servicio}")
    public ResponseEntity<Map<String, Object>> respaldo(@PathVariable String servicio) {
        String nombre = NOMBRES.getOrDefault(servicio, servicio);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "SERVICIO_NO_DISPONIBLE",
                "mensaje", "el servicio de " + nombre + " no esta disponible; reintente en unos segundos",
                "fecha", LocalDateTime.now().toString()));
    }
}
