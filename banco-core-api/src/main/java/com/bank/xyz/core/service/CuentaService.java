package com.bank.xyz.core.service;

import com.bank.xyz.core.dto.CuentaDto;
import com.bank.xyz.core.dto.EstadoAnualDto;
import com.bank.xyz.core.dto.InteresDto;
import com.bank.xyz.core.dto.MovimientoDto;
import com.bank.xyz.core.dto.PaginaDto;
import com.bank.xyz.core.dto.SaldoDto;
import com.bank.xyz.core.exception.RecursoNoEncontradoException;
import com.bank.xyz.core.model.Cuenta;
import com.bank.xyz.core.model.EstadoCuentaAnual;
import com.bank.xyz.core.model.InteresCalculado;
import com.bank.xyz.core.model.MovimientoAnual;
import com.bank.xyz.core.repository.CuentaRepository;
import com.bank.xyz.core.repository.EstadoCuentaAnualRepository;
import com.bank.xyz.core.repository.InteresCalculadoRepository;
import com.bank.xyz.core.repository.MovimientoAnualRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CuentaService {

    private final CuentaRepository cuentas;
    private final MovimientoAnualRepository movimientos;
    private final EstadoCuentaAnualRepository estados;
    private final InteresCalculadoRepository intereses;

    public CuentaService(CuentaRepository cuentas,
                         MovimientoAnualRepository movimientos,
                         EstadoCuentaAnualRepository estados,
                         InteresCalculadoRepository intereses) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.estados = estados;
        this.intereses = intereses;
    }

    public PaginaDto<CuentaDto> listar(Pageable pageable) {
        return PaginaDto.de(cuentas.findAll(pageable), CuentaService::aDto);
    }

    public CuentaDto obtener(Integer cuentaId) {
        return aDto(buscar(cuentaId));
    }

    public SaldoDto saldo(Integer cuentaId) {
        Cuenta cuenta = buscar(cuentaId);
        return new SaldoDto(cuenta.getCuentaId(), cuenta.getSaldoFinal(), LocalDateTime.now());
    }

    public PaginaDto<MovimientoDto> movimientos(Integer cuentaId, Integer anio, Pageable pageable) {
        verificarExiste(cuentaId);
        Page<MovimientoAnual> pagina = (anio == null)
                ? movimientos.findByCuentaIdOrderByFechaDesc(cuentaId, pageable)
                : movimientos.findByCuentaIdAndAnioOrderByFechaDesc(cuentaId, anio, pageable);
        return PaginaDto.de(pagina, CuentaService::aDto);
    }

    public List<MovimientoDto> ultimosMovimientos(Integer cuentaId, int cantidad) {
        verificarExiste(cuentaId);
        return movimientos.findTop10ByCuentaIdOrderByFechaDescIdDesc(cuentaId).stream()
                .limit(cantidad)
                .map(CuentaService::aDto)
                .toList();
    }

    public List<EstadoAnualDto> estadosAnuales(Integer cuentaId) {
        verificarExiste(cuentaId);
        return estados.findByCuentaIdOrderByAnioDesc(cuentaId).stream()
                .map(CuentaService::aDto)
                .toList();
    }

    public EstadoAnualDto estadoAnual(Integer cuentaId, Integer anio) {
        verificarExiste(cuentaId);
        return estados.findByCuentaIdAndAnio(cuentaId, anio)
                .map(CuentaService::aDto)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "la cuenta " + cuentaId + " no tiene estado de cuenta del anio " + anio));
    }

    public PaginaDto<InteresDto> intereses(Integer cuentaId, Pageable pageable) {
        verificarExiste(cuentaId);
        return PaginaDto.de(intereses.findByCuentaId(cuentaId, pageable), CuentaService::aDto);
    }

    private Cuenta buscar(Integer cuentaId) {
        return cuentas.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("no existe la cuenta " + cuentaId));
    }

    private void verificarExiste(Integer cuentaId) {
        if (!cuentas.existsById(cuentaId)) {
            throw new RecursoNoEncontradoException("no existe la cuenta " + cuentaId);
        }
    }

    private static CuentaDto aDto(Cuenta c) {
        return new CuentaDto(c.getCuentaId(), c.getNombre(), c.getTipo(), c.getSaldoFinal(),
                c.getInteresTotal(), c.getRegistrosProcesados(), c.getActualizadoEn());
    }

    private static MovimientoDto aDto(MovimientoAnual m) {
        return new MovimientoDto(m.getId(), m.getCuentaId(), m.getAnio(), m.getFecha(),
                m.getTipoMovimiento(), m.getMonto(), m.getDescripcion(),
                m.getEstado(), m.getObservaciones());
    }

    private static EstadoAnualDto aDto(EstadoCuentaAnual e) {
        return new EstadoAnualDto(e.getCuentaId(), e.getAnio(), e.getCantidadMovimientos(),
                e.getTotalDepositos(), e.getTotalRetiros(), e.getTotalCompras(),
                e.getTotalPagos(), e.getSaldoNeto(), e.getMovimientosConAnomalia());
    }

    private static InteresDto aDto(InteresCalculado i) {
        return new InteresDto(i.getId(), i.getCuentaId(), i.getTipoCuenta(), i.getSaldoInicial(),
                i.getTasaAnual(), i.getInteresMensual(), i.getSaldoFinal(), i.getEstado());
    }
}
