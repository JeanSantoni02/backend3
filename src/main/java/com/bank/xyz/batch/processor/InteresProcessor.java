package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.config.InteresProperties;
import com.bank.xyz.batch.dto.InteresCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.model.EstadoRegistro;
import com.bank.xyz.batch.model.InteresCalculado;
import com.bank.xyz.batch.util.NumeroParser;
import com.bank.xyz.batch.util.TextoNormalizador;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringJoiner;


@Component
public class InteresProcessor implements ItemProcessor<InteresCsv, InteresCalculado> {

    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 120;
    private static final BigDecimal MESES_DEL_ANIO = new BigDecimal("12");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final InteresProperties tasas;

    public InteresProcessor(InteresProperties tasas) {
        this.tasas = tasas;
    }

    @Override
    public InteresCalculado process(InteresCsv fila) {
        StringJoiner observaciones = new StringJoiner("; ");
        EstadoRegistro estado = EstadoRegistro.VALIDO;

        Integer cuentaId = NumeroParser.aEntero(fila.getCuentaId());
        if (cuentaId == null) {
            throw new RegistroInvalidoException("cuenta_id ausente o no numerico", fila.toString());
        }

        BigDecimal saldo = NumeroParser.aImporte(fila.getSaldo());
        if (saldo == null) {
            throw new RegistroInvalidoException(
                    "saldo ausente: no hay capital sobre el cual calcular interes", fila.toString());
        }

        String tipo = TextoNormalizador.normalizar(fila.getTipo());
        BigDecimal tasaAnual = tasas.tasaDe(tipo);
        if (tasaAnual == null) {
            throw new RegistroInvalidoException(
                    "tipo de cuenta no reconocido (" + fila.getTipo() + "): sin tasa aplicable",
                    fila.toString());
        }

        Integer edad = NumeroParser.aEntero(fila.getEdad());
        if (edad == null) {
            estado = EstadoRegistro.CORREGIDO;
            observaciones.add("edad ausente");
        } else if (edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
            estado = EstadoRegistro.CORREGIDO;
            observaciones.add("edad fuera de rango (" + edad + ")");
            edad = null;
        }

        String nombre = fila.getNombre();
        if (TextoNormalizador.vacio(nombre) || "unknown".equals(TextoNormalizador.normalizar(nombre))) {
            estado = EstadoRegistro.CORREGIDO;
            observaciones.add("titular sin identificar");
            nombre = "SIN IDENTIFICAR";
        }

        BigDecimal interesMensual = saldo
                .multiply(tasaAnual)
                .divide(CIEN, 10, RoundingMode.HALF_UP)
                .divide(MESES_DEL_ANIO, 2, RoundingMode.HALF_UP);

        InteresCalculado calculo = new InteresCalculado();
        calculo.setCuentaId(cuentaId);
        calculo.setNombre(nombre);
        calculo.setTipoCuenta(tipo);
        calculo.setEdad(edad);
        calculo.setSaldoInicial(saldo.setScale(2, RoundingMode.HALF_UP));
        calculo.setTasaAnual(tasaAnual);
        calculo.setInteresMensual(interesMensual);
        calculo.setSaldoFinal(saldo.add(interesMensual).setScale(2, RoundingMode.HALF_UP));
        calculo.setEstado(estado);
        calculo.setObservaciones(observaciones.length() == 0 ? null : observaciones.toString());
        return calculo;
    }
}
