package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.dto.MovimientoAnualCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.model.EstadoRegistro;
import com.bank.xyz.batch.model.MovimientoAnual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovimientoAnualProcessorTest {

    private final MovimientoAnualProcessor processor = new MovimientoAnualProcessor();

    private MovimientoAnualCsv fila(String cuentaId, String fecha, String transaccion,
                                    String monto, String descripcion) {
        MovimientoAnualCsv csv = new MovimientoAnualCsv();
        csv.setCuentaId(cuentaId);
        csv.setFecha(fecha);
        csv.setTransaccion(transaccion);
        csv.setMonto(monto);
        csv.setDescripcion(descripcion);
        return csv;
    }

    @Test
    @DisplayName("deposito con tilde y sin tilde quedan en la misma categoria")
    void normalizaDepositoAcentuado() {
        MovimientoAnual conTilde = processor.process(
                fila("103", "08-03-2024", "depósito", "3000", "Ingreso mensual"));
        MovimientoAnual sinTilde = processor.process(
                fila("103", "08-03-2024", "deposito", "3000", "Ingreso mensual"));

        assertEquals("deposito", conTilde.getTipoMovimiento());
        assertEquals(sinTilde.getTipoMovimiento(), conTilde.getTipoMovimiento());
        assertEquals("depósito", conTilde.getTipoOriginal());
    }

    @Test
    @DisplayName("el monto negativo se normaliza a positivo porque el signo lo da el tipo")
    void normalizaMontoNegativo() {
        MovimientoAnual resultado = processor.process(
                fila("106", "12/02/2024", "deposito", "-100", "Ingreso navideno"));

        assertEquals(new BigDecimal("100"), resultado.getMonto());
        assertEquals(EstadoRegistro.CORREGIDO, resultado.getEstado());
        assertTrue(resultado.getObservaciones().contains("-100"));
    }

    @Test
    @DisplayName("la descripcion vacia se completa y se marca como corregida")
    void completaDescripcionVacia() {
        MovimientoAnual resultado = processor.process(
                fila("110", "24-07-2024", "retiro", "1500", ""));

        assertEquals("SIN DESCRIPCION", resultado.getDescripcion());
        assertEquals(EstadoRegistro.CORREGIDO, resultado.getEstado());
    }

    @Test
    @DisplayName("el monto vacio descarta el movimiento")
    void descartaMontoVacio() {
        assertThrows(RegistroInvalidoException.class,
                () -> processor.process(fila("120", "2024/04/11", "compra", "", "Ingreso mensual")));
    }

    @Test
    @DisplayName("deriva el anio desde la fecha para agrupar el estado de cuenta")
    void derivaElAnio() {
        MovimientoAnual resultado = processor.process(
                fila("109", "2024/03/18", "deposito", "3000", "Ingreso mensual"));

        assertEquals(2024, resultado.getAnio());
    }
}
