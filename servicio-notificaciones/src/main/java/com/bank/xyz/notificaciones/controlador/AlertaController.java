package com.bank.xyz.notificaciones.controlador;

import com.bank.xyz.notificaciones.modelo.AlertaSeguridad;
import com.bank.xyz.notificaciones.repositorio.RepositorioAlertas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas de seguridad", description = "Alertas consumidas del topico de seguridad")
public class AlertaController {

    private final RepositorioAlertas repositorio;

    public AlertaController(RepositorioAlertas repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    @Operation(summary = "Ultimas alertas recibidas")
    public List<AlertaSeguridad> ultimas(@RequestParam(defaultValue = "20") int cantidad) {
        return repositorio.ultimas(Math.min(cantidad, 100));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Cantidad de alertas recibidas")
    public Map<String, Object> resumen() {
        return Map.of("totalAlertas", repositorio.total());
    }
}
