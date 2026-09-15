package com.bank.xyz.bff.atm.servicio;

import com.bank.xyz.bff.atm.cliente.ClienteCoreApi;
import com.bank.xyz.bff.atm.cliente.CoreDto;
import com.bank.xyz.bff.atm.config.AtmProperties;
import com.bank.xyz.bff.atm.error.OperacionRechazadaException;
import com.bank.xyz.bff.atm.modelo.ComprobanteRetiro;
import com.bank.xyz.bff.atm.modelo.RetiroAtmRequest;
import com.bank.xyz.bff.atm.modelo.SaldoAtm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CajeroService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA_ATM");

    private final ClienteCoreApi core;
    private final AtmProperties propiedades;

    public CajeroService(ClienteCoreApi core, AtmProperties propiedades) {
        this.core = core;
        this.propiedades = propiedades;
    }

    public SaldoAtm consultarSaldo(Integer cuentaId, String terminal) {
        CoreDto.Saldo saldo = core.saldo(cuentaId);
        BigDecimal disponible = saldo.saldo() == null ? BigDecimal.ZERO : saldo.saldo();

        auditoria.info("CONSULTA_SALDO terminal={} cuenta={}", terminal, cuentaId);

        return new SaldoAtm(
                saldo.cuentaId(),
                disponible,
                maximoRetirable(disponible),
                propiedades.getDenominacion(),
                LocalDateTime.now());
    }

    public ComprobanteRetiro retirar(Integer cuentaId, RetiroAtmRequest peticion, String terminal) {
        BigDecimal monto = peticion.monto();
        validar(monto);

        String referencia = (peticion.referencia() == null || peticion.referencia().isBlank())
                ? generarReferencia(terminal)
                : peticion.referencia();

        auditoria.info("RETIRO_SOLICITADO terminal={} cuenta={} monto={} referencia={}",
                terminal, cuentaId, monto, referencia);

        CoreDto.RetiroResponse respuesta =
                core.retirar(cuentaId, new CoreDto.RetiroRequest(monto, referencia));

        auditoria.info("RETIRO_CONFIRMADO terminal={} cuenta={} monto={} referencia={} "
                        + "saldo={} duplicado={}",
                terminal, cuentaId, respuesta.montoRetirado(), referencia,
                respuesta.saldoResultante(), respuesta.reintentoIdempotente());

        return new ComprobanteRetiro(
                referencia,
                respuesta.cuentaId(),
                respuesta.montoRetirado(),
                respuesta.saldoResultante(),
                terminal,
                respuesta.reintentoIdempotente(),
                respuesta.ocurridoEn());
    }

    // Se valida aqui, antes de llamar al servicio de dominio, porque el cajero
    // fisico no puede entregar cualquier monto: solo billetes de la denominacion
    // que tiene cargada. Rechazar temprano ahorra una llamada de red y le da al
    // usuario un mensaje inmediato.
    private void validar(BigDecimal monto) {
        if (monto.compareTo(propiedades.getMontoMinimo()) < 0) {
            throw new OperacionRechazadaException("MONTO_MINIMO",
                    "el monto minimo de retiro es " + propiedades.getMontoMinimo().toPlainString());
        }
        if (monto.compareTo(propiedades.getMontoMaximo()) > 0) {
            throw new OperacionRechazadaException("MONTO_MAXIMO",
                    "el monto maximo por operacion es " + propiedades.getMontoMaximo().toPlainString());
        }
        if (monto.remainder(propiedades.getDenominacion()).compareTo(BigDecimal.ZERO) != 0) {
            throw new OperacionRechazadaException("DENOMINACION",
                    "este cajero solo entrega billetes de "
                            + propiedades.getDenominacion().toPlainString());
        }
    }

    private BigDecimal maximoRetirable(BigDecimal disponible) {
        BigDecimal tope = disponible.min(propiedades.getMontoMaximo());
        if (tope.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        // Redondeado hacia abajo a la denominacion: no sirve ofrecer 3500 si el
        // cajero solo tiene billetes de 1000.
        return tope.subtract(tope.remainder(propiedades.getDenominacion()));
    }

    private String generarReferencia(String terminal) {
        return terminal + "-" + UUID.randomUUID();
    }
}
