package com.bank.xyz.pagos.servicio;

import com.bank.xyz.pagos.dto.PagoDto;
import com.bank.xyz.pagos.error.ManejadorErrores.NoEncontradoException;
import com.bank.xyz.pagos.error.ManejadorErrores.PagoRechazadoException;
import com.bank.xyz.pagos.evento.AlertaSeguridad;
import com.bank.xyz.pagos.evento.EventoTransaccion;
import com.bank.xyz.pagos.evento.PublicadorEventos;
import com.bank.xyz.pagos.modelo.Pago;
import com.bank.xyz.pagos.modelo.Pago.TipoPago;
import com.bank.xyz.pagos.modelo.SaldoCuenta;
import com.bank.xyz.pagos.repositorio.PagoRepository;
import com.bank.xyz.pagos.repositorio.SaldoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    public record Resultado(Pago pago, boolean duplicado) {
    }

    private final PagoRepository pagos;
    private final SaldoRepository saldos;
    private final PublicadorEventos publicador;
    private final BigDecimal umbralAlerta;

    public PagoService(PagoRepository pagos, SaldoRepository saldos, PublicadorEventos publicador,
                       @Value("${banco.pagos.umbral-alerta:1000000}") BigDecimal umbralAlerta) {
        this.pagos = pagos;
        this.saldos = saldos;
        this.publicador = publicador;
        this.umbralAlerta = umbralAlerta;
    }

    @Transactional
    public Resultado depositar(PagoDto.Deposito d) {
        SaldoCuenta cuenta = bloquear(d.cuentaId());

        // Tras el bloqueo una peticion repetida ya ve la anterior confirmada y no deposita dos veces
        Optional<Pago> previo = pagos.findByReferencia(d.referencia());
        if (previo.isPresent()) {
            return new Resultado(previo.get(), true);
        }

        exigirOperativa(cuenta, d.monto(), d.referencia());
        cuenta.abonar(d.monto());

        Pago pago = pagos.save(new Pago(d.referencia(), TipoPago.DEPOSITO, null, d.cuentaId(),
                d.monto(), d.glosa(), cuenta.saldo()));

        publicador.transaccionAlConfirmar(evento(d.referencia(), "DEPOSITO", cuenta, d.monto()));
        revisarMonto(d.referencia(), d.cuentaId(), d.monto());

        log.info("Deposito aplicado | ref={} cuenta={} monto={} saldo={}",
                d.referencia(), d.cuentaId(), d.monto(), cuenta.saldo());
        return new Resultado(pago, false);
    }

    @Transactional
    public Resultado transferir(PagoDto.Transferencia t) {
        if (t.cuentaOrigen().equals(t.cuentaDestino())) {
            throw new PagoRechazadoException("MISMA_CUENTA", "la cuenta de origen y destino son la misma");
        }

        // Siempre en el mismo orden para que dos transferencias cruzadas no se esperen mutuamente
        Integer menor = Math.min(t.cuentaOrigen(), t.cuentaDestino());
        Integer mayor = Math.max(t.cuentaOrigen(), t.cuentaDestino());
        SaldoCuenta primera = bloquear(menor);
        SaldoCuenta segunda = bloquear(mayor);
        SaldoCuenta origen = primera.getCuentaId().equals(t.cuentaOrigen()) ? primera : segunda;
        SaldoCuenta destino = origen == primera ? segunda : primera;

        Optional<Pago> previo = pagos.findByReferencia(t.referencia());
        if (previo.isPresent()) {
            return new Resultado(previo.get(), true);
        }

        exigirOperativa(origen, t.monto(), t.referencia());
        exigirOperativa(destino, t.monto(), t.referencia());
        exigirSaldo(origen, t.monto());

        // Debito y abono en la misma transaccion: o pasan los dos o ninguno
        origen.debitar(t.monto());
        destino.abonar(t.monto());

        Pago pago = pagos.save(new Pago(t.referencia(), TipoPago.TRANSFERENCIA, t.cuentaOrigen(),
                t.cuentaDestino(), t.monto(), t.glosa(), origen.saldo()));

        publicador.transaccionAlConfirmar(
                evento(t.referencia() + "-O", "TRANSFERENCIA_ENVIADA", origen, t.monto()));
        publicador.transaccionAlConfirmar(
                evento(t.referencia() + "-D", "TRANSFERENCIA_RECIBIDA", destino, t.monto()));
        revisarMonto(t.referencia(), t.cuentaOrigen(), t.monto());

        log.info("Transferencia aplicada | ref={} {} -> {} monto={}",
                t.referencia(), t.cuentaOrigen(), t.cuentaDestino(), t.monto());
        return new Resultado(pago, false);
    }

    @Transactional
    public Resultado pagar(PagoDto.PagoServicio p) {
        SaldoCuenta cuenta = bloquear(p.cuentaId());

        Optional<Pago> previo = pagos.findByReferencia(p.referencia());
        if (previo.isPresent()) {
            return new Resultado(previo.get(), true);
        }

        exigirOperativa(cuenta, p.monto(), p.referencia());
        exigirSaldo(cuenta, p.monto());
        cuenta.debitar(p.monto());

        Pago pago = pagos.save(new Pago(p.referencia(), TipoPago.PAGO, p.cuentaId(), null,
                p.monto(), p.convenio(), cuenta.saldo()));

        publicador.transaccionAlConfirmar(evento(p.referencia(), "PAGO", cuenta, p.monto()));
        revisarMonto(p.referencia(), p.cuentaId(), p.monto());

        log.info("Pago aplicado | ref={} cuenta={} convenio={} monto={}",
                p.referencia(), p.cuentaId(), p.convenio(), p.monto());
        return new Resultado(pago, false);
    }

    @Transactional(readOnly = true)
    public Pago porReferencia(String referencia) {
        return pagos.findByReferencia(referencia)
                .orElseThrow(() -> new NoEncontradoException("no existe la operacion " + referencia));
    }

    @Transactional(readOnly = true)
    public List<Pago> deCuenta(Integer cuentaId) {
        return pagos.deCuenta(cuentaId);
    }

    private SaldoCuenta bloquear(Integer cuentaId) {
        return saldos.bloquear(cuentaId)
                .orElseThrow(() -> new NoEncontradoException("no existe la cuenta " + cuentaId));
    }

    // Operar sobre una cuenta bloqueada o cerrada es sospechoso: se rechaza y se avisa
    private void exigirOperativa(SaldoCuenta cuenta, BigDecimal monto, String referencia) {
        if (cuenta.operativa()) {
            return;
        }
        publicador.alertaInmediata(new AlertaSeguridad(
                "ALR-" + referencia + "-" + cuenta.getCuentaId(), "CUENTA_NO_OPERATIVA", "ALTA",
                cuenta.getCuentaId(), monto,
                "intento de operar sobre una cuenta en estado " + cuenta.estadoEfectivo()));
        throw new PagoRechazadoException("CUENTA_NO_OPERATIVA",
                "la cuenta " + cuenta.getCuentaId() + " esta " + cuenta.estadoEfectivo());
    }

    private void exigirSaldo(SaldoCuenta cuenta, BigDecimal monto) {
        if (cuenta.saldo().compareTo(monto) < 0) {
            throw new PagoRechazadoException("SALDO_INSUFICIENTE",
                    "saldo insuficiente en la cuenta " + cuenta.getCuentaId());
        }
    }

    // El movimiento se aplica igual; la alerta es para que alguien lo revise
    private void revisarMonto(String referencia, Integer cuentaId, BigDecimal monto) {
        if (monto.compareTo(umbralAlerta) >= 0) {
            publicador.alertaAlConfirmar(new AlertaSeguridad(
                    "ALR-" + referencia, "MONTO_INUSUAL", "MEDIA", cuentaId, monto,
                    "operacion igual o superior al umbral de " + umbralAlerta));
        }
    }

    private EventoTransaccion evento(String id, String tipo, SaldoCuenta cuenta, BigDecimal monto) {
        return new EventoTransaccion(id, tipo, cuenta.getCuentaId(), monto, cuenta.saldo(),
                id, "PAGOS", LocalDateTime.now());
    }
}
