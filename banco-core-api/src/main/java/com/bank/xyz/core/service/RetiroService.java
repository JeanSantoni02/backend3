package com.bank.xyz.core.service;

import com.bank.xyz.core.dto.RetiroRequest;
import com.bank.xyz.core.dto.RetiroResponse;
import com.bank.xyz.core.exception.OperacionInvalidaException;
import com.bank.xyz.core.exception.RecursoNoEncontradoException;
import com.bank.xyz.core.exception.SaldoInsuficienteException;
import com.bank.xyz.core.model.Cuenta;
import com.bank.xyz.core.model.OperacionAtm;
import com.bank.xyz.core.repository.CuentaRepository;
import com.bank.xyz.core.repository.OperacionAtmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RetiroService {

    private static final Logger log = LoggerFactory.getLogger(RetiroService.class);
    private static final String TIPO_RETIRO = "RETIRO";

    private final CuentaRepository cuentas;
    private final OperacionAtmRepository operaciones;

    private final BigDecimal montoMaximo;
    private final BigDecimal multiplo;

    public RetiroService(CuentaRepository cuentas,
                         OperacionAtmRepository operaciones,
                         @Value("${banco.atm.monto-maximo:200000}") BigDecimal montoMaximo,
                         @Value("${banco.atm.multiplo:1000}") BigDecimal multiplo) {
        this.cuentas = cuentas;
        this.operaciones = operaciones;
        this.montoMaximo = montoMaximo;
        this.multiplo = multiplo;
    }

    @Transactional
    public RetiroResponse retirar(Integer cuentaId, RetiroRequest peticion) {
        // Idempotencia: si el cajero reintenta por timeout de red, se devuelve el
        // resultado de la operacion original en vez de cobrar dos veces.
        var previa = operaciones.findByReferencia(peticion.referencia());
        if (previa.isPresent()) {
            OperacionAtm op = previa.get();
            if (!op.getCuentaId().equals(cuentaId)) {
                throw new OperacionInvalidaException(
                        "la referencia " + peticion.referencia() + " ya fue usada por otra cuenta");
            }
            log.info("Retiro idempotente: la referencia {} ya estaba procesada", peticion.referencia());
            return respuesta(op, true);
        }

        validarMonto(peticion.monto());

        Cuenta cuenta = cuentas.buscarParaActualizar(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("no existe la cuenta " + cuentaId));

        BigDecimal saldoAnterior = cuenta.getSaldoFinal() == null ? BigDecimal.ZERO : cuenta.getSaldoFinal();
        if (saldoAnterior.compareTo(peticion.monto()) < 0) {
            throw new SaldoInsuficienteException(saldoAnterior);
        }

        BigDecimal saldoResultante = saldoAnterior.subtract(peticion.monto());
        cuenta.setSaldoFinal(saldoResultante);
        cuenta.setActualizadoEn(LocalDateTime.now());

        OperacionAtm operacion = new OperacionAtm();
        operacion.setCuentaId(cuentaId);
        operacion.setTipo(TIPO_RETIRO);
        operacion.setMonto(peticion.monto());
        operacion.setSaldoAnterior(saldoAnterior);
        operacion.setSaldoResultante(saldoResultante);
        operacion.setReferencia(peticion.referencia());
        operaciones.save(operacion);

        log.info("Retiro cuenta={} monto={} saldo {} -> {}",
                cuentaId, peticion.monto(), saldoAnterior, saldoResultante);

        return respuesta(operacion, false);
    }

    private void validarMonto(BigDecimal monto) {
        if (monto.compareTo(montoMaximo) > 0) {
            throw new OperacionInvalidaException(
                    "el monto supera el maximo por operacion (" + montoMaximo.toPlainString() + ")");
        }
        if (monto.remainder(multiplo).compareTo(BigDecimal.ZERO) != 0) {
            throw new OperacionInvalidaException(
                    "el cajero solo entrega multiplos de " + multiplo.toPlainString());
        }
    }

    private RetiroResponse respuesta(OperacionAtm op, boolean reintento) {
        return new RetiroResponse(op.getId(), op.getCuentaId(), op.getMonto(),
                op.getSaldoAnterior(), op.getSaldoResultante(), op.getReferencia(),
                reintento, op.getOcurridoEn());
    }
}
