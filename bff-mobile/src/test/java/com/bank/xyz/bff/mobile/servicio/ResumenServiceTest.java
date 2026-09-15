package com.bank.xyz.bff.mobile.servicio;

import com.bank.xyz.bff.mobile.cliente.ClienteCoreApi;
import com.bank.xyz.bff.mobile.cliente.CoreDto;
import com.bank.xyz.bff.mobile.modelo.ResumenMovil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumenServiceTest {

    private ClienteCoreApi core;
    private ResumenService servicio;

    @BeforeEach
    void preparar() {
        core = mock(ClienteCoreApi.class);
        servicio = new ResumenService(core);
    }

    private CoreDto.Movimiento movimiento(String descripcion) {
        return new CoreDto.Movimiento(1L, LocalDate.of(2024, 5, 20), "deposito",
                new BigDecimal("2500"), descripcion);
    }

    @Test
    @DisplayName("pide solo cinco movimientos, no el historial completo")
    void pideSoloCincoMovimientos() {
        when(core.cuenta(101)).thenReturn(
                new CoreDto.Cuenta(101, "Bob Johnson", "hipoteca", new BigDecimal("125814.17")));
        when(core.ultimosMovimientos(eq(101), anyInt())).thenReturn(List.of());

        servicio.resumen(101);

        verify(core).ultimosMovimientos(101, 5);
    }

    @Test
    @DisplayName("acorta las descripciones largas para no gastar datos del telefono")
    void acortaDescripcionesLargas() {
        when(core.cuenta(101)).thenReturn(
                new CoreDto.Cuenta(101, "Bob Johnson", "hipoteca", new BigDecimal("100")));
        when(core.ultimosMovimientos(eq(101), anyInt())).thenReturn(
                List.of(movimiento("Compra en tienda de articulos deportivos del centro")));

        ResumenMovil resumen = servicio.resumen(101);
        String descripcion = resumen.ultimos().get(0).d();

        assertEquals(24, descripcion.length());
        assertTrue(descripcion.endsWith("…"));
    }

    @Test
    @DisplayName("deja la descripcion en null cuando viene vacia, para que no se serialice")
    void omiteDescripcionVacia() {
        when(core.cuenta(101)).thenReturn(
                new CoreDto.Cuenta(101, "Bob Johnson", "hipoteca", new BigDecimal("100")));
        when(core.ultimosMovimientos(eq(101), anyInt())).thenReturn(List.of(movimiento("")));

        assertNull(servicio.resumen(101).ultimos().get(0).d());
    }

    @Test
    @DisplayName("no acorta descripciones que ya caben en pantalla")
    void conservaDescripcionesCortas() {
        when(core.cuenta(101)).thenReturn(
                new CoreDto.Cuenta(101, "Bob Johnson", "hipoteca", new BigDecimal("100")));
        when(core.ultimosMovimientos(eq(101), anyInt())).thenReturn(
                List.of(movimiento("Ingreso mensual")));

        assertEquals("Ingreso mensual", servicio.resumen(101).ultimos().get(0).d());
    }

    @Test
    @DisplayName("la consulta de saldo solo devuelve id y saldo")
    void saldoMinimo() {
        when(core.saldo(101)).thenReturn(new CoreDto.Saldo(101, new BigDecimal("125814.17")));

        var saldo = servicio.saldo(101);

        assertEquals(101, saldo.id());
        assertEquals(new BigDecimal("125814.17"), saldo.saldo());
    }
}
