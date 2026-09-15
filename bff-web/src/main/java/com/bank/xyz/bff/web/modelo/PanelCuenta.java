package com.bank.xyz.bff.web.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Respuesta unica del portal web: trae de una sola vez todo lo que la pantalla
// de detalle necesita pintar. Es lo contrario del BFF movil, que devuelve lo
// minimo: aqui el cliente tiene ancho de banda y pantalla grande, y lo que
// importa es evitarle seis llamadas encadenadas.
public record PanelCuenta(
        Titular titular,
        Resumen resumen,
        List<EstadoAnual> estadosAnuales,
        ResumenIntereses intereses,
        PaginaMovimientos movimientos,
        Metadatos meta) {

    public record Titular(Integer cuentaId, String nombre, String tipoCuenta, boolean identificado) {
    }

    public record Resumen(BigDecimal saldoActual,
                          BigDecimal interesAcumulado,
                          Long registrosProcesados,
                          LocalDateTime actualizadoEn) {
    }

    public record EstadoAnual(Integer anio,
                              Long cantidadMovimientos,
                              BigDecimal totalDepositos,
                              BigDecimal totalRetiros,
                              BigDecimal totalCompras,
                              BigDecimal totalPagos,
                              BigDecimal saldoNeto,
                              Long movimientosConAnomalia) {
    }

    public record ResumenIntereses(String tipoCuenta,
                                   BigDecimal tasaAnual,
                                   int registros,
                                   BigDecimal interesMensualTotal) {
    }

    public record Movimiento(Long id,
                             LocalDate fecha,
                             String tipo,
                             BigDecimal monto,
                             String descripcion,
                             String estado,
                             String observaciones) {
    }

    public record PaginaMovimientos(List<Movimiento> items,
                                    int pagina,
                                    int tamano,
                                    long total,
                                    int totalPaginas,
                                    boolean ultima) {
    }

    public record Metadatos(LocalDateTime generadoEn, long milisegundos, List<String> avisos) {
    }
}
