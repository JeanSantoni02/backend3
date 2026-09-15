package com.bank.xyz.core.controller;

import com.bank.xyz.core.dto.PaginaDto;
import com.bank.xyz.core.dto.ResumenDiarioDto;
import com.bank.xyz.core.service.TransaccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transacciones")
@Tag(name = "Transacciones", description = "Resumen diario de transacciones")
public class TransaccionController {

    private final TransaccionService servicio;

    public TransaccionController(TransaccionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen-diario")
    @Operation(summary = "Resumen diario, opcionalmente acotado por rango de fechas")
    public PaginaDto<ResumenDiarioDto> resumenDiario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 30) Pageable pageable) {
        return servicio.resumenDiario(desde, hasta, pageable);
    }
}
