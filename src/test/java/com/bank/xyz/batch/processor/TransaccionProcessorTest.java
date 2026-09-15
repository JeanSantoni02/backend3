package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.dto.TransaccionCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.model.EstadoRegistro;
import com.bank.xyz.batch.model.Transaccion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransaccionProcessorTest {

    private final TransaccionProcessor processor = new TransaccionProcessor();

    private TransaccionCsv fila(String id, String fecha, String monto, String tipo) {
        TransaccionCsv csv = new TransaccionCsv();
        csv.setId(id);
        csv.setFecha(fecha);
        csv.setMonto(monto);
        csv.setTipo(tipo);
        return csv;
    }

    @Test
    @DisplayName("una transaccion limpia queda como VALIDO")
    void procesaTransaccionValida() {
        Transaccion resultado = processor.process(fila("1", "2024-06-30", "3000", "credito"));

        assertEquals(1L, resultado.getId());
        assertEquals(LocalDate.of(2024, 6, 30), resultado.getFecha());
        assertEquals(EstadoRegistro.VALIDO, resultado.getEstado());
    }

    @Test
    @DisplayName("la fecha imposible 2024-13-01 descarta la fila del reporte diario")
    void descartaFechaImposible() {
        assertThrows(RegistroInvalidoException.class,
                () -> processor.process(fila("7", "2024-13-01", "700", "debito")));
    }

    @Test
    @DisplayName("el monto vacio descarta la fila")
    void descartaMontoVacio() {
        assertThrows(RegistroInvalidoException.class,
                () -> processor.process(fila("4", "04/05/2024", "", "invalid")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "desconocido", ""})
    @DisplayName("el tipo fuera del catalogo se conserva marcado como anomalia")
    void marcaTipoFueraDeCatalogo(String tipo) {
        Transaccion resultado = processor.process(fila("3", "2024-04-09", "800", tipo));

        assertEquals("no_clasificado", resultado.getTipo());
        assertEquals(EstadoRegistro.ANOMALIA, resultado.getEstado());
        assertTrue(resultado.getObservaciones().contains("tipo fuera del catalogo"));
    }

    @Test
    @DisplayName("el monto negativo se conserva, marcado como anomalia")
    void marcaMontoNegativo() {
        Transaccion resultado = processor.process(fila("9", "07-04-2024", "-3000", "debito"));

        assertEquals(EstadoRegistro.ANOMALIA, resultado.getEstado());
        assertTrue(resultado.getObservaciones().contains("monto negativo"));
    }

    @Test
    @DisplayName("conserva el texto original de la fecha para auditoria")
    void conservaFechaOriginal() {
        Transaccion resultado = processor.process(fila("2", "03-04-2024", "1200", "credito"));

        assertEquals("03-04-2024", resultado.getFechaOriginal());
        assertEquals(LocalDate.of(2024, 4, 3), resultado.getFecha());
    }
}
