package com.bank.xyz.bff.web.servicio;

import com.bank.xyz.bff.web.cliente.ClienteCoreApi;
import com.bank.xyz.bff.web.cliente.CoreDto;
import com.bank.xyz.bff.web.error.CoreApiNoDisponibleException;
import com.bank.xyz.bff.web.modelo.PanelCuenta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PanelServiceTest {

    private ClienteCoreApi core;
    private ExecutorService ejecutor;
    private PanelService servicio;

    @BeforeEach
    void preparar() {
        core = mock(ClienteCoreApi.class);
        ejecutor = Executors.newFixedThreadPool(4);
        servicio = new PanelService(core, ejecutor);
    }

    @AfterEach
    void cerrar() {
        ejecutor.shutdownNow();
    }

    private void respuestasBasicas(String nombre, long anomalias) {
        when(core.cuenta(101)).thenReturn(new CoreDto.Cuenta(101, nombre, "hipoteca",
                new BigDecimal("125814.17"), new BigDecimal("814.17"), 13L, LocalDateTime.now()));
        when(core.estadosAnuales(101)).thenReturn(List.of(new CoreDto.EstadoAnual(
                101, 2024, 44L, new BigDecimal("38000"), new BigDecimal("10800"),
                new BigDecimal("12700"), new BigDecimal("1600"), new BigDecimal("12900"), anomalias)));
        when(core.intereses(eq(101), anyInt(), anyInt())).thenReturn(new CoreDto.Pagina<>(
                List.of(new CoreDto.Interes(1L, 101, "hipoteca", new BigDecimal("12000"),
                        new BigDecimal("8.0"), new BigDecimal("80.00"),
                        new BigDecimal("12080.00"), "VALIDO")),
                0, 100, 1, 1, true));
        when(core.movimientos(eq(101), any(), anyInt(), anyInt())).thenReturn(new CoreDto.Pagina<>(
                List.of(new CoreDto.Movimiento(1L, 101, 2024, LocalDate.of(2024, 5, 1), "deposito",
                        new BigDecimal("2500"), "Ingreso mensual", "VALIDO", null)),
                0, 20, 1, 1, true));
    }

    @Test
    @DisplayName("arma el panel completo en una sola respuesta")
    void armaPanelCompleto() {
        respuestasBasicas("Bob Johnson", 0L);

        PanelCuenta panel = servicio.panel(101, null, 0, 20);

        assertEquals(101, panel.titular().cuentaId());
        assertTrue(panel.titular().identificado());
        assertEquals(new BigDecimal("125814.17"), panel.resumen().saldoActual());
        assertEquals(1, panel.estadosAnuales().size());
        assertEquals(new BigDecimal("8.0"), panel.intereses().tasaAnual());
        assertEquals(1, panel.movimientos().items().size());
    }

    @Test
    @DisplayName("avisa cuando el titular no pudo ser identificado en la migracion")
    void avisaTitularSinIdentificar() {
        respuestasBasicas("SIN IDENTIFICAR", 0L);

        PanelCuenta panel = servicio.panel(101, null, 0, 20);

        assertFalse(panel.titular().identificado());
        assertTrue(panel.meta().avisos().stream().anyMatch(a -> a.contains("no pudo ser identificado")));
    }

    @Test
    @DisplayName("avisa cuando hay movimientos marcados como anomalia")
    void avisaAnomalias() {
        respuestasBasicas("Bob Johnson", 3L);

        PanelCuenta panel = servicio.panel(101, null, 0, 20);

        assertTrue(panel.meta().avisos().stream().anyMatch(a -> a.contains("3 movimiento")));
    }

    @Test
    @DisplayName("si el servicio de dominio falla, propaga la causa real y no un error generico")
    void propagaLaCausaReal() {
        respuestasBasicas("Bob Johnson", 0L);
        when(core.estadosAnuales(101))
                .thenThrow(new CoreApiNoDisponibleException("sin conexion"));

        // Sin desenvolver el CompletionException, aqui llegaria un
        // CompletionException y el portal respondería 500 en vez de 503.
        assertThrows(CoreApiNoDisponibleException.class, () -> servicio.panel(101, null, 0, 20));
    }
}
