package com.bank.xyz.core.service;

import com.bank.xyz.core.dto.PaginaDto;
import com.bank.xyz.core.dto.ResumenDiarioDto;
import com.bank.xyz.core.model.ResumenDiario;
import com.bank.xyz.core.repository.ResumenDiarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class TransaccionService {

    private final ResumenDiarioRepository resumenes;

    public TransaccionService(ResumenDiarioRepository resumenes) {
        this.resumenes = resumenes;
    }

    public PaginaDto<ResumenDiarioDto> resumenDiario(LocalDate desde, LocalDate hasta, Pageable pageable) {
        Page<ResumenDiario> pagina = (desde != null && hasta != null)
                ? resumenes.findByFechaBetweenOrderByFechaDesc(desde, hasta, pageable)
                : resumenes.findAllByOrderByFechaDesc(pageable);
        return PaginaDto.de(pagina, TransaccionService::aDto);
    }

    private static ResumenDiarioDto aDto(ResumenDiario r) {
        return new ResumenDiarioDto(r.getFecha(), r.getTotalTransacciones(), r.getMontoTotal(),
                r.getMontoPromedio(), r.getMontoMaximo(), r.getCantidadAnomalias());
    }
}
