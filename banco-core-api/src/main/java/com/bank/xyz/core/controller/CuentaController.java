package com.bank.xyz.core.controller;

import com.bank.xyz.core.dto.CuentaDto;
import com.bank.xyz.core.dto.EstadoAnualDto;
import com.bank.xyz.core.dto.InteresDto;
import com.bank.xyz.core.dto.MovimientoDto;
import com.bank.xyz.core.dto.PaginaDto;
import com.bank.xyz.core.dto.SaldoDto;
import com.bank.xyz.core.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cuentas")
@Tag(name = "Cuentas", description = "Datos de cuentas generados por el batch de la semana 3")
public class CuentaController {

    private final CuentaService servicio;

    public CuentaController(CuentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista paginada de cuentas")
    public PaginaDto<CuentaDto> listar(@PageableDefault(size = 20) Pageable pageable) {
        return servicio.listar(pageable);
    }

    @GetMapping("/{cuentaId}")
    @Operation(summary = "Detalle de una cuenta")
    public CuentaDto obtener(@PathVariable Integer cuentaId) {
        return servicio.obtener(cuentaId);
    }

    @GetMapping("/{cuentaId}/saldo")
    @Operation(summary = "Saldo actual de la cuenta")
    public SaldoDto saldo(@PathVariable Integer cuentaId) {
        return servicio.saldo(cuentaId);
    }

    @GetMapping("/{cuentaId}/movimientos")
    @Operation(summary = "Movimientos de la cuenta, opcionalmente filtrados por anio")
    public PaginaDto<MovimientoDto> movimientos(@PathVariable Integer cuentaId,
                                                @RequestParam(required = false) Integer anio,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return servicio.movimientos(cuentaId, anio, pageable);
    }

    @GetMapping("/{cuentaId}/movimientos/ultimos")
    @Operation(summary = "Ultimos movimientos, pensado para clientes livianos")
    public List<MovimientoDto> ultimos(@PathVariable Integer cuentaId,
                                       @RequestParam(defaultValue = "5") int cantidad) {
        return servicio.ultimosMovimientos(cuentaId, Math.min(cantidad, 10));
    }

    @GetMapping("/{cuentaId}/estados-anuales")
    @Operation(summary = "Estados de cuenta anuales")
    public List<EstadoAnualDto> estadosAnuales(@PathVariable Integer cuentaId) {
        return servicio.estadosAnuales(cuentaId);
    }

    @GetMapping("/{cuentaId}/estados-anuales/{anio}")
    @Operation(summary = "Estado de cuenta de un anio especifico")
    public EstadoAnualDto estadoAnual(@PathVariable Integer cuentaId, @PathVariable Integer anio) {
        return servicio.estadoAnual(cuentaId, anio);
    }

    @GetMapping("/{cuentaId}/intereses")
    @Operation(summary = "Intereses calculados para la cuenta")
    public PaginaDto<InteresDto> intereses(@PathVariable Integer cuentaId,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return servicio.intereses(cuentaId, pageable);
    }
}
