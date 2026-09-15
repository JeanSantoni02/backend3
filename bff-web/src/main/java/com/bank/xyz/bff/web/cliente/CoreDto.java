package com.bank.xyz.bff.web.cliente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Reflejo de lo que expone banco-core-api. Cada BFF declara el suyo a proposito:
// asi un cambio en el contrato del servicio de dominio no obliga a modificar los
// tres BFF a la vez, solo el que realmente usa el campo que cambio.
public final class CoreDto {

    private CoreDto() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cuenta(Integer cuentaId, String nombre, String tipo, BigDecimal saldoFinal,
                         BigDecimal interesTotal, Long registrosProcesados,
                         LocalDateTime actualizadoEn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Movimiento(Long id, Integer cuentaId, Integer anio, LocalDate fecha, String tipo,
                             BigDecimal monto, String descripcion, String estado,
                             String observaciones) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EstadoAnual(Integer cuentaId, Integer anio, Long cantidadMovimientos,
                              BigDecimal totalDepositos, BigDecimal totalRetiros,
                              BigDecimal totalCompras, BigDecimal totalPagos,
                              BigDecimal saldoNeto, Long movimientosConAnomalia) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Interes(Long id, Integer cuentaId, String tipoCuenta, BigDecimal saldoInicial,
                          BigDecimal tasaAnual, BigDecimal interesMensual, BigDecimal saldoFinal,
                          String estado) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos,
                            int totalPaginas, boolean ultima) {
    }
}
