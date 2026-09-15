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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetiroServiceTest {

    private CuentaRepository cuentas;
    private OperacionAtmRepository operaciones;
    private RetiroService servicio;

    @BeforeEach
    void preparar() {
        cuentas = mock(CuentaRepository.class);
        operaciones = mock(OperacionAtmRepository.class);
        servicio = new RetiroService(cuentas, operaciones,
                new BigDecimal("200000"), new BigDecimal("1000"));
        when(operaciones.findByReferencia(any())).thenReturn(Optional.empty());
    }

    private Cuenta cuentaCon(BigDecimal saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setCuentaId(101);
        cuenta.setSaldoFinal(saldo);
        return cuenta;
    }

    @Test
    @DisplayName("descuenta el monto del saldo y registra la operacion")
    void descuentaElSaldo() {
        Cuenta cuenta = cuentaCon(new BigDecimal("50000"));
        when(cuentas.buscarParaActualizar(101)).thenReturn(Optional.of(cuenta));

        RetiroResponse respuesta = servicio.retirar(101,
                new RetiroRequest(new BigDecimal("20000"), "REF-1"));

        assertEquals(new BigDecimal("30000"), cuenta.getSaldoFinal());
        assertEquals(new BigDecimal("30000"), respuesta.saldoResultante());
        assertFalse(respuesta.reintentoIdempotente());
        verify(operaciones).save(any(OperacionAtm.class));
    }

    @Test
    @DisplayName("una referencia repetida devuelve la operacion original sin volver a debitar")
    void esIdempotente() {
        OperacionAtm previa = new OperacionAtm();
        previa.setCuentaId(101);
        previa.setMonto(new BigDecimal("20000"));
        previa.setSaldoAnterior(new BigDecimal("50000"));
        previa.setSaldoResultante(new BigDecimal("30000"));
        previa.setReferencia("REF-1");
        when(operaciones.findByReferencia("REF-1")).thenReturn(Optional.of(previa));

        RetiroResponse respuesta = servicio.retirar(101,
                new RetiroRequest(new BigDecimal("20000"), "REF-1"));

        assertTrue(respuesta.reintentoIdempotente());
        assertEquals(new BigDecimal("30000"), respuesta.saldoResultante());
        // Lo esencial: no vuelve a tocar la cuenta ni guarda otra operacion.
        verify(cuentas, never()).buscarParaActualizar(any());
        verify(operaciones, never()).save(any());
    }

    @Test
    @DisplayName("rechaza reutilizar una referencia de otra cuenta")
    void rechazaReferenciaDeOtraCuenta() {
        OperacionAtm previa = new OperacionAtm();
        previa.setCuentaId(999);
        previa.setReferencia("REF-1");
        when(operaciones.findByReferencia("REF-1")).thenReturn(Optional.of(previa));

        assertThrows(OperacionInvalidaException.class,
                () -> servicio.retirar(101, new RetiroRequest(new BigDecimal("20000"), "REF-1")));
    }

    @Test
    @DisplayName("no permite dejar la cuenta en negativo")
    void rechazaSaldoInsuficiente() {
        when(cuentas.buscarParaActualizar(101)).thenReturn(Optional.of(cuentaCon(new BigDecimal("5000"))));

        assertThrows(SaldoInsuficienteException.class,
                () -> servicio.retirar(101, new RetiroRequest(new BigDecimal("20000"), "REF-2")));
        verify(operaciones, never()).save(any());
    }

    @Test
    @DisplayName("valida denominacion y tope antes de tocar la cuenta")
    void validaAntesDeBloquearLaCuenta() {
        assertThrows(OperacionInvalidaException.class,
                () -> servicio.retirar(101, new RetiroRequest(new BigDecimal("2500"), "REF-3")));
        assertThrows(OperacionInvalidaException.class,
                () -> servicio.retirar(101, new RetiroRequest(new BigDecimal("300000"), "REF-4")));

        verify(cuentas, never()).buscarParaActualizar(any());
    }

    @Test
    @DisplayName("falla si la cuenta no existe")
    void fallaSiNoExisteLaCuenta() {
        when(cuentas.buscarParaActualizar(101)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.retirar(101, new RetiroRequest(new BigDecimal("5000"), "REF-5")));
    }
}
