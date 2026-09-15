package com.bank.xyz.core.controller;

import com.bank.xyz.core.dto.RetiroRequest;
import com.bank.xyz.core.dto.RetiroResponse;
import com.bank.xyz.core.service.RetiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/{cuentaId}/retiros")
@Tag(name = "Retiros", description = "Operacion critica: debita el saldo de la cuenta")
public class RetiroController {

    private final RetiroService servicio;

    public RetiroController(RetiroService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Registra un retiro. Idempotente por el campo referencia")
    public RetiroResponse retirar(@PathVariable Integer cuentaId,
                                  @Valid @RequestBody RetiroRequest peticion) {
        return servicio.retirar(cuentaId, peticion);
    }
}
