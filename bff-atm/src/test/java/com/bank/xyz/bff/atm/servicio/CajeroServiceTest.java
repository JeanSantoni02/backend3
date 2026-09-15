package com.bank.xyz.bff.atm.servicio;

import com.bank.xyz.bff.atm.cliente.ClienteCoreApi;
import com.bank.xyz.bff.atm.cliente.CoreDto;
import com.bank.xyz.bff.atm.config.AtmProperties;
import com.bank.xyz.bff.atm.error.OperacionRechazadaException;
import com.bank.xyz.bff.atm.modelo.ComprobanteRetiro;
import com.bank.xyz.bff.atm.modelo.RetiroAtmRequest;
import com.bank.xyz.bff.atm.modelo.SaldoAtm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CajeroServiceTest {

    private static final String TERMINAL = "ATM-001";

    private ClienteCoreApi core;
    private CajeroService servicio;

    @BeforeEach
    void preparar() {
        core = mock(ClienteCoreApi.class);
        servicio = new CajeroService(core, new AtmProperties());
    }

    @Test
    @DisplayName("el maximo retirable se redondea hacia abajo a la denominacion")
    void redondeaMaximoRetirable() {
        when(core.saldo(101)).thenReturn(
                new CoreDto.Saldo(101, new BigDecimal("125814.17"), LocalDateTime.now()));

        SaldoAtm saldo = servicio.consultarSaldo(101, TERMINAL);

        // El tope por operacion es 200000, pero el saldo es menor; se baja al
        // multiplo de 1000 mas cercano por debajo.
        assertEquals(new BigDecimal("125000.00"), saldo.maximoRetirable());
        assertEquals(new BigDecimal("125814.17"), saldo.saldoDisponible());
    }

    @Test
    @DisplayName("el maximo retirable nunca supera el tope por operacion")
    void respetaTopePorOperacion() {
        when(core.saldo(101)).thenReturn(
                new CoreDto.Saldo(101, new BigDecimal("5000000"), LocalDateTime.now()));

        assertEquals(new BigDecimal("200000"), servicio.consultarSaldo(101, TERMINAL).maximoRetirable());
    }

    @Test
    @DisplayName("rechaza montos que el cajero no puede entregar en billetes")
    void rechazaDenominacionInvalida() {
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> servicio.retirar(101, new RetiroAtmRequest(new BigDecimal("2500"), "R1"), TERMINAL));

        assertEquals("DENOMINACION", e.getCodigo());
        // No debe siquiera intentar la llamada al servicio de dominio.
        verify(core, never()).retirar(any(), any());
    }

    @Test
    @DisplayName("rechaza montos bajo el minimo y sobre el maximo")
    void rechazaMontosFueraDeRango() {
        assertEquals("MONTO_MINIMO", assertThrows(OperacionRechazadaException.class,
                () -> servicio.retirar(101, new RetiroAtmRequest(new BigDecimal("500"), "R2"), TERMINAL))
                .getCodigo());

        assertEquals("MONTO_MAXIMO", assertThrows(OperacionRechazadaException.class,
                () -> servicio.retirar(101, new RetiroAtmRequest(new BigDecimal("300000"), "R3"), TERMINAL))
                .getCodigo());
    }

    @Test
    @DisplayName("genera una referencia propia cuando el cajero no la envia")
    void generaReferenciaSiFalta() {
        when(core.retirar(eq(101), any())).thenAnswer(invocacion -> {
            CoreDto.RetiroRequest peticion = invocacion.getArgument(1);
            return new CoreDto.RetiroResponse(1L, 101, peticion.monto(),
                    new BigDecimal("50000"), new BigDecimal("45000"),
                    peticion.referencia(), false, LocalDateTime.now());
        });

        ComprobanteRetiro comprobante =
                servicio.retirar(101, new RetiroAtmRequest(new BigDecimal("5000"), null), TERMINAL);

        ArgumentCaptor<CoreDto.RetiroRequest> captor =
                ArgumentCaptor.forClass(CoreDto.RetiroRequest.class);
        verify(core).retirar(eq(101), captor.capture());

        assertTrue(captor.getValue().referencia().startsWith(TERMINAL + "-"));
        assertEquals(captor.getValue().referencia(), comprobante.comprobante());
        assertFalse(comprobante.duplicado());
    }

    @Test
    @DisplayName("respeta la referencia que envia el cajero, para poder reintentar")
    void conservaLaReferenciaDelCajero() {
        when(core.retirar(eq(101), any())).thenReturn(new CoreDto.RetiroResponse(
                7L, 101, new BigDecimal("5000"), new BigDecimal("50000"),
                new BigDecimal("45000"), "TICKET-77", true, LocalDateTime.now()));

        ComprobanteRetiro comprobante = servicio.retirar(
                101, new RetiroAtmRequest(new BigDecimal("5000"), "TICKET-77"), TERMINAL);

        assertEquals("TICKET-77", comprobante.comprobante());
        assertTrue(comprobante.duplicado());
    }
}
