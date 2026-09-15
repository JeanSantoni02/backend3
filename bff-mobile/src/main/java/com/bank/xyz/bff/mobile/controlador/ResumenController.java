package com.bank.xyz.bff.mobile.controlador;

import com.bank.xyz.bff.mobile.modelo.ResumenMovil;
import com.bank.xyz.bff.mobile.modelo.SaldoMovil;
import com.bank.xyz.bff.mobile.servicio.ResumenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/movil")
@Tag(name = "BFF Movil", description = "Respuestas minimas para la app de telefono")
public class ResumenController {

    private final ResumenService servicio;

    public ResumenController(ResumenService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cuentas/{cuentaId}")
    @Operation(summary = "Pantalla principal: saldo y ultimos cinco movimientos")
    public ResumenMovil resumen(@PathVariable Integer cuentaId) {
        return servicio.resumen(cuentaId);
    }

    @GetMapping("/cuentas/{cuentaId}/saldo")
    @Operation(summary = "Solo el saldo, la consulta mas frecuente de la app")
    public SaldoMovil saldo(@PathVariable Integer cuentaId) {
        return servicio.saldo(cuentaId);
    }
}
