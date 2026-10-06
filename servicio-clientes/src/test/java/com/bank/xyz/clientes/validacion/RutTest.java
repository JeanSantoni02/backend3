package com.bank.xyz.clientes.validacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RutTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345678-5", "11111111-1", "9876543-3", "12.345.678-5", "16789012-1"})
    void aceptaRutConDigitoCorrecto(String rut) {
        assertThat(Rut.esValido(rut)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678-4", "15234567-8", "123-4", "abcdefgh-1", "12345678"})
    void rechazaRutMalFormadoODigitoIncorrecto(String rut) {
        assertThat(Rut.esValido(rut)).isFalse();
    }

    @Test
    void aceptaDigitoKEnMinuscula() {
        // 10000013-K: el resto da 10 y el digito es K
        assertThat(Rut.esValido("10000013-k")).isTrue();
    }

    @Test
    void normalizaPuntosYEspacios() {
        assertThat(Rut.normalizar(" 12.345.678-5 ")).isEqualTo("12345678-5");
    }
}
