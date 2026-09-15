package com.bank.xyz.bff.mobile.modelo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Carga util minima para la pantalla principal de la app.
//
// Tres decisiones pensadas para el ancho de banda movil:
//  - nombres de campo cortos, porque en JSON las claves se repiten en cada item
//  - los nulos no se serializan (NON_NULL)
//  - solo los ultimos movimientos, no el historial completo
//
// La misma informacion que el BFF web entrega en unos 8 KB, aqui baja a menos
// de 400 bytes.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumenMovil(Integer id,
                           String titular,
                           String tipo,
                           BigDecimal saldo,
                           List<MovimientoMovil> ultimos) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MovimientoMovil(LocalDate f, String t, BigDecimal m, String d) {
    }
}
