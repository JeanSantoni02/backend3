package com.bank.xyz.bff.mobile.servicio;

import com.bank.xyz.bff.mobile.cliente.ClienteCoreApi;
import com.bank.xyz.bff.mobile.cliente.CoreDto;
import com.bank.xyz.bff.mobile.modelo.ResumenMovil;
import com.bank.xyz.bff.mobile.modelo.SaldoMovil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResumenService {

    private static final int MAXIMO_MOVIMIENTOS = 5;
    private static final int LARGO_DESCRIPCION = 24;

    private final ClienteCoreApi core;

    public ResumenService(ClienteCoreApi core) {
        this.core = core;
    }

    public SaldoMovil saldo(Integer cuentaId) {
        CoreDto.Saldo saldo = core.saldo(cuentaId);
        return new SaldoMovil(saldo.cuentaId(), saldo.saldo());
    }

    public ResumenMovil resumen(Integer cuentaId) {
        CoreDto.Cuenta cuenta = core.cuenta(cuentaId);
        List<CoreDto.Movimiento> movimientos = core.ultimosMovimientos(cuentaId, MAXIMO_MOVIMIENTOS);

        return new ResumenMovil(
                cuenta.cuentaId(),
                cuenta.nombre(),
                cuenta.tipo(),
                cuenta.saldoFinal(),
                movimientos.stream().map(ResumenService::aMovimiento).toList());
    }

    private static ResumenMovil.MovimientoMovil aMovimiento(CoreDto.Movimiento m) {
        return new ResumenMovil.MovimientoMovil(m.fecha(), m.tipo(), m.monto(),
                acortar(m.descripcion()));
    }

    // La pantalla del movil no alcanza a mostrar descripciones largas, asi que
    // no tiene sentido transmitirlas enteras.
    private static String acortar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.length() <= LARGO_DESCRIPCION
                ? texto
                : texto.substring(0, LARGO_DESCRIPCION - 1) + "\u2026";
    }
}
