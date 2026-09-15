package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.config.InteresProperties;
import com.bank.xyz.batch.dto.InteresCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.model.EstadoRegistro;
import com.bank.xyz.batch.model.InteresCalculado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InteresProcessorTest {

    private InteresProcessor processor;

    @BeforeEach
    void prepararProcessor() {
        processor = new InteresProcessor(new InteresProperties());
    }

    private InteresCsv fila(String cuentaId, String nombre, String saldo, String edad, String tipo) {
        InteresCsv csv = new InteresCsv();
        csv.setCuentaId(cuentaId);
        csv.setNombre(nombre);
        csv.setSaldo(saldo);
        csv.setEdad(edad);
        csv.setTipo(tipo);
        return csv;
    }

    @Test
    @DisplayName("aplica 2.5% anual a las cuentas de ahorro")
    void calculaInteresDeAhorro() {
        InteresCalculado resultado = processor.process(
                fila("101", "Alice Brown", "12000", "40", "ahorro"));

        // 12000 * 2.5% / 12 = 25.00
        assertEquals(new BigDecimal("25.00"), resultado.getInteresMensual());
        assertEquals(new BigDecimal("12025.00"), resultado.getSaldoFinal());
        assertEquals(EstadoRegistro.VALIDO, resultado.getEstado());
    }

    @Test
    @DisplayName("el interes de un prestamo aumenta el saldo adeudado")
    void calculaInteresDePrestamo() {
        InteresCalculado resultado = processor.process(
                fila("102", "Bob Johnson", "10000", "30", "prestamo"));

        // 10000 * 12% / 12 = 100.00
        assertEquals(new BigDecimal("100.00"), resultado.getInteresMensual());
        assertEquals(new BigDecimal("10100.00"), resultado.getSaldoFinal());
    }

    @Test
    @DisplayName("descarta la fila cuando no hay saldo sobre el cual calcular")
    void descartaSaldoAusente() {
        assertThrows(RegistroInvalidoException.class,
                () -> processor.process(fila("103", "Jane Smith", "", "35", "hipoteca")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "unknown", ""})
    @DisplayName("descarta la fila cuando el tipo no tiene tasa aplicable")
    void descartaTipoNoReconocido(String tipo) {
        assertThrows(RegistroInvalidoException.class,
                () -> processor.process(fila("104", "Bob Johnson", "7000", "25", tipo)));
    }

    @Test
    @DisplayName("la edad 150 no impide el calculo, se corrige y se documenta")
    void corrigeEdadFueraDeRango() {
        InteresCalculado resultado = processor.process(
                fila("105", "Steve Rogers", "8000", "150", "hipoteca"));

        assertNull(resultado.getEdad());
        assertEquals(EstadoRegistro.CORREGIDO, resultado.getEstado());
        assertEquals(new BigDecimal("8053.33"), resultado.getSaldoFinal());
    }

    @Test
    @DisplayName("marca como corregido al titular sin identificar")
    void corrigeNombreDesconocido() {
        InteresCalculado resultado = processor.process(
                fila("106", "Unknown", "5000", "45", "ahorro"));

        assertEquals("SIN IDENTIFICAR", resultado.getNombre());
        assertEquals(EstadoRegistro.CORREGIDO, resultado.getEstado());
    }
}
