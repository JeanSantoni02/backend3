package com.bank.xyz.notificaciones.controlador;

import com.bank.xyz.notificaciones.modelo.Notificacion;
import com.bank.xyz.notificaciones.repositorio.RepositorioNotificaciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notificaciones")
@Tag(name = "Notificaciones", description = "Eventos de transaccion consumidos del broker")
public class NotificacionController {

    private final RepositorioNotificaciones repositorio;

    public NotificacionController(RepositorioNotificaciones repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    @Operation(summary = "Ultimas notificaciones generadas")
    public List<Notificacion> ultimas(@RequestParam(defaultValue = "20") int cantidad) {
        return repositorio.ultimas(Math.min(cantidad, 100));
    }

    @GetMapping("/cuentas/{cuentaId}")
    @Operation(summary = "Notificaciones de una cuenta")
    public List<Notificacion> porCuenta(@PathVariable Integer cuentaId) {
        return repositorio.porCuenta(cuentaId);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Cantidad de eventos procesados")
    public Map<String, Object> resumen() {
        return Map.of("totalNotificaciones", repositorio.total());
    }
}
