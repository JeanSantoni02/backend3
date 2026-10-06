package com.bank.xyz.pagos.servicio;

import com.bank.xyz.pagos.dto.PagoDto;
import com.bank.xyz.pagos.error.ManejadorErrores.PagoRechazadoException;
import com.bank.xyz.pagos.evento.AlertaSeguridad;
import com.bank.xyz.pagos.evento.EventoTransaccion;
import com.bank.xyz.pagos.evento.PublicadorEventos;
import com.bank.xyz.pagos.modelo.Pago;
import com.bank.xyz.pagos.modelo.SaldoCuenta;
import com.bank.xyz.pagos.repositorio.PagoRepository;
import com.bank.xyz.pagos.repositorio.SaldoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoServiceTest {

    private PagoRepository pagos;
    private SaldoRepository saldos;
    private PublicadorEventos publicador;
    private PagoService servicio;

    @BeforeEach
    void preparar() {
        pagos = mock(PagoRepository.class);
        saldos = mock(SaldoRepository.class);
        publicador = mock(PublicadorEventos.class);
        servicio = new PagoService(pagos, saldos, publicador, new BigDecimal("1000000"));
        when(pagos.findByReferencia(any())).thenReturn(Optional.empty());
        when(pagos.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
    }

    private SaldoCuenta cuenta(int id, String saldo, String estado) {
        SaldoCuenta c = new SaldoCuenta();
        ReflectionTestUtils.setField(c, "cuentaId", id);
        ReflectionTestUtils.setField(c, "saldoFinal", new BigDecimal(saldo));
        ReflectionTestUtils.setField(c, "estado", estado);
        when(saldos.bloquear(id)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void depositoAbonaYPublicaElEvento() {
        SaldoCuenta c = cuenta(101, "10000", null);

        var r = servicio.depositar(new PagoDto.Deposito(101, new BigDecimal("5000"), "DEP-1", null));

        assertThat(c.saldo()).isEqualByComparingTo("15000");
        assertThat(r.duplicado()).isFalse();
        verify(publicador).transaccionAlConfirmar(any(EventoTransaccion.class));
    }

    @Test
    void transferenciaDebitaYAbonaEnLaMismaOperacion() {
        SaldoCuenta origen = cuenta(101, "50000", "ACTIVA");
        SaldoCuenta destino = cuenta(102, "1000", null);

        servicio.transferir(new PagoDto.Transferencia(101, 102, new BigDecimal("20000"), "TRF-1", null));

        assertThat(origen.saldo()).isEqualByComparingTo("30000");
        assertThat(destino.saldo()).isEqualByComparingTo("21000");
        verify(publicador, times(2)).transaccionAlConfirmar(any(EventoTransaccion.class));
    }

    @Test
    void transferenciaBloqueaLasCuentasSiempreEnElMismoOrden() {
        cuenta(101, "0", null);
        cuenta(250, "50000", null);

        // El origen es la 250, pero igual se bloquea primero la 101
        servicio.transferir(new PagoDto.Transferencia(250, 101, new BigDecimal("1000"), "TRF-ORD", null));

        var orden = inOrder(saldos);
        orden.verify(saldos).bloquear(101);
        orden.verify(saldos).bloquear(250);
    }

    @Test
    void rechazaSinSaldoSuficienteYNoGuardaNada() {
        cuenta(101, "1000", null);
        cuenta(102, "0", null);

        assertThatThrownBy(() -> servicio.transferir(
                new PagoDto.Transferencia(101, 102, new BigDecimal("5000"), "TRF-2", null)))
                .isInstanceOf(PagoRechazadoException.class)
                .hasMessageContaining("saldo insuficiente");
        verify(pagos, never()).save(any());
    }

    @Test
    void cuentaBloqueadaSeRechazaYGeneraAlertaInmediata() {
        cuenta(101, "90000", "BLOQUEADA");

        assertThatThrownBy(() -> servicio.depositar(
                new PagoDto.Deposito(101, new BigDecimal("100"), "DEP-B", null)))
                .isInstanceOf(PagoRechazadoException.class);

        ArgumentCaptor<AlertaSeguridad> alerta = ArgumentCaptor.forClass(AlertaSeguridad.class);
        verify(publicador).alertaInmediata(alerta.capture());
        assertThat(alerta.getValue().getTipo()).isEqualTo("CUENTA_NO_OPERATIVA");
    }

    @Test
    void montoSobreElUmbralSeAplicaPeroDisparaAlerta() {
        cuenta(101, "0", null);

        servicio.depositar(new PagoDto.Deposito(101, new BigDecimal("1500000"), "DEP-GRANDE", null));

        ArgumentCaptor<AlertaSeguridad> alerta = ArgumentCaptor.forClass(AlertaSeguridad.class);
        verify(publicador).alertaAlConfirmar(alerta.capture());
        assertThat(alerta.getValue().getTipo()).isEqualTo("MONTO_INUSUAL");
    }

    @Test
    void referenciaRepetidaDevuelveLaOperacionOriginalSinVolverAAplicarla() {
        SaldoCuenta c = cuenta(101, "10000", null);
        Pago original = new Pago("DEP-1", Pago.TipoPago.DEPOSITO, null, 101,
                new BigDecimal("5000"), null, new BigDecimal("15000"));
        when(pagos.findByReferencia("DEP-1")).thenReturn(Optional.of(original));

        var r = servicio.depositar(new PagoDto.Deposito(101, new BigDecimal("5000"), "DEP-1", null));

        assertThat(r.duplicado()).isTrue();
        assertThat(c.saldo()).isEqualByComparingTo("10000");
        verify(pagos, never()).save(any());
    }

    @Test
    void noSePuedeTransferirALaMismaCuenta() {
        assertThatThrownBy(() -> servicio.transferir(
                new PagoDto.Transferencia(101, 101, new BigDecimal("10"), "TRF-X", null)))
                .isInstanceOf(PagoRechazadoException.class)
                .hasMessageContaining("misma");
    }
}
