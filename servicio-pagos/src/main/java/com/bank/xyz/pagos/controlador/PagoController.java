package com.bank.xyz.pagos.controlador;

import com.bank.xyz.pagos.dto.PagoDto;
import com.bank.xyz.pagos.servicio.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "Depositos, transferencias y pagos de servicios")
public class PagoController {

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/depositos")
    @Operation(summary = "Deposito en una cuenta. Idempotente por referencia")
    public ResponseEntity<PagoDto.Respuesta> depositar(@Valid @RequestBody PagoDto.Deposito datos) {
        return responder(servicio.depositar(datos));
    }

    @PostMapping("/transferencias")
    @Operation(summary = "Transferencia entre cuentas en una sola transaccion")
    public ResponseEntity<PagoDto.Respuesta> transferir(@Valid @RequestBody PagoDto.Transferencia datos) {
        return responder(servicio.transferir(datos));
    }

    @PostMapping("/servicios")
    @Operation(summary = "Pago de un servicio o convenio con cargo a la cuenta")
    public ResponseEntity<PagoDto.Respuesta> pagar(@Valid @RequestBody PagoDto.PagoServicio datos) {
        return responder(servicio.pagar(datos));
    }

    @GetMapping("/{referencia}")
    @Operation(summary = "Consulta una operacion por su referencia")
    public PagoDto.Respuesta porReferencia(@PathVariable String referencia) {
        return PagoDto.Respuesta.de(servicio.porReferencia(referencia), false);
    }

    @GetMapping
    @Operation(summary = "Operaciones de una cuenta, como origen o destino")
    public List<PagoDto.Respuesta> deCuenta(@RequestParam Integer cuentaId) {
        return servicio.deCuenta(cuentaId).stream().map(p -> PagoDto.Respuesta.de(p, false)).toList();
    }

    // 201 la primera vez; 200 si la referencia ya se habia procesado
    private ResponseEntity<PagoDto.Respuesta> responder(PagoService.Resultado r) {
        HttpStatus estado = r.duplicado() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(PagoDto.Respuesta.de(r.pago(), r.duplicado()));
    }
}
