package com.bank.xyz.batch.model;

/**
 * Resultado de pasar un registro por el ItemProcessor.
 *
 * Es importante que CORREGIDO y ANOMALIA sean estados distintos: el negocio
 * necesita saber cuantos datos tuvo que arreglar el batch (calidad del legacy)
 * y cuantos quedaron marcados para revision manual (riesgo operativo).
 */
public enum EstadoRegistro {
    /** Paso todas las validaciones sin tocar nada. */
    VALIDO,
    /** Tenia un problema subsanable y el processor lo normalizo. */
    CORREGIDO,
    /** Se persiste, pero queda marcado para revision (monto negativo, fecha imposible). */
    ANOMALIA
}
