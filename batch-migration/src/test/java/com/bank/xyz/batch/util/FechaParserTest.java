package com.bank.xyz.batch.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FechaParserTest {

    @ParameterizedTest
    @CsvSource({
            "2024-06-30, 2024-06-30",
            "2024/10/15, 2024-10-15",
            "03-04-2024, 2024-04-03",
            "12/02/2024, 2024-02-12"
    })
    @DisplayName("reconoce los cuatro formatos que trae el archivo oficial")
    void reconoceLosCuatroFormatos(String entrada, String esperado) {
        assertEquals(LocalDate.parse(esperado), FechaParser.parsear(entrada));
    }

    @Test
    @DisplayName("interpreta los formatos de dos digitos como dia primero")
    void interpretaDiaPrimero() {
        assertEquals(LocalDate.of(2024, 7, 30), FechaParser.parsear("30-07-2024"));
        assertEquals(LocalDate.of(2024, 3, 24), FechaParser.parsear("24/03/2024"));
    }

    @Test
    @DisplayName("rechaza el mes 13 en vez de corregirlo en silencio")
    void rechazaMesImposible() {
        assertNull(FechaParser.parsear("2024-13-01"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "no es fecha", "2024-02-30"})
    @DisplayName("devuelve null ante entradas vacias o invalidas")
    void devuelveNullAnteEntradasInvalidas(String entrada) {
        assertNull(FechaParser.parsear(entrada));
    }

    @Test
    void devuelveNullAnteNull() {
        assertNull(FechaParser.parsear(null));
    }
}
