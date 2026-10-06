package com.bank.xyz.cuentas.controlador;

import com.bank.xyz.cuentas.dto.CuentaDto;
import com.bank.xyz.cuentas.servicio.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cuentas")
@Tag(name = "Gestion de cuentas", description = "Apertura, cierre y mantenimiento de cuentas")
public class CuentaController {

    private final CuentaService servicio;

    public CuentaController(CuentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una cuenta, migrada o abierta en linea")
    public CuentaDto.Respuesta obtener(@PathVariable Integer id) {
        return CuentaDto.Respuesta.de(servicio.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Cuentas de un cliente")
    public List<CuentaDto.Respuesta> deCliente(@RequestParam Long clienteId) {
        return servicio.deCliente(clienteId).stream().map(CuentaDto.Respuesta::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una cuenta. Valida al titular contra el servicio de clientes")
    public CuentaDto.Respuesta abrir(@Valid @RequestBody CuentaDto.Apertura datos) {
        return CuentaDto.Respuesta.de(servicio.abrir(datos));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Mantenimiento: cambia el tipo o bloquea y desbloquea la cuenta")
    public CuentaDto.Respuesta mantener(@PathVariable Integer id,
                                        @Valid @RequestBody CuentaDto.Mantenimiento datos) {
        return CuentaDto.Respuesta.de(servicio.mantener(id, datos));
    }

    @PostMapping("/{id}/cierre")
    @Operation(summary = "Cierra la cuenta. Exige saldo cero")
    public CuentaDto.Respuesta cerrar(@PathVariable Integer id) {
        return CuentaDto.Respuesta.de(servicio.cerrar(id));
    }
}
