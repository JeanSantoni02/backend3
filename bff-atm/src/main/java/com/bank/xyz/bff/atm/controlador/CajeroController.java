package com.bank.xyz.bff.atm.controlador;

import com.bank.xyz.bff.atm.modelo.ComprobanteRetiro;
import com.bank.xyz.bff.atm.modelo.RetiroAtmRequest;
import com.bank.xyz.bff.atm.modelo.SaldoAtm;
import com.bank.xyz.bff.atm.servicio.CajeroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/atm")
@Tag(name = "BFF Cajeros", description = "Operaciones criticas. Requiere credencial de terminal")
public class CajeroController {

    private final CajeroService servicio;

    public CajeroController(CajeroService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cuentas/{cuentaId}/saldo")
    @Operation(summary = "Consulta de saldo con el maximo retirable ya calculado")
    public SaldoAtm saldo(@PathVariable Integer cuentaId,
                          @AuthenticationPrincipal String terminal) {
        return servicio.consultarSaldo(cuentaId, terminal);
    }

    @PostMapping("/cuentas/{cuentaId}/retiro")
    @Operation(summary = "Retiro de efectivo. Idempotente si se repite la referencia")
    public ComprobanteRetiro retirar(@PathVariable Integer cuentaId,
                                     @Valid @RequestBody RetiroAtmRequest peticion,
                                     @AuthenticationPrincipal String terminal) {
        return servicio.retirar(cuentaId, peticion, terminal);
    }
}
