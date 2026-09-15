package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.dto.TransaccionCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.model.EstadoRegistro;
import com.bank.xyz.batch.model.Transaccion;
import com.bank.xyz.batch.util.FechaParser;
import com.bank.xyz.batch.util.NumeroParser;
import com.bank.xyz.batch.util.TextoNormalizador;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;


@Component
public class TransaccionProcessor implements ItemProcessor<TransaccionCsv, Transaccion> {

    private static final List<String> TIPOS_VALIDOS = List.of("credito", "debito");
    private static final String TIPO_NO_CLASIFICADO = "no_clasificado";

    @Override
    public Transaccion process(TransaccionCsv fila) {
        StringJoiner observaciones = new StringJoiner("; ");
        EstadoRegistro estado = EstadoRegistro.VALIDO;

        Long id = leerId(fila);
        LocalDate fecha = leerFecha(fila);
        BigDecimal monto = leerMonto(fila);

        // --- Anomalias de monto: se conservan, marcadas ---
        if (monto.signum() < 0) {
            estado = EstadoRegistro.ANOMALIA;
            observaciones.add("monto negativo (" + monto.toPlainString() + ")");
        } else if (monto.signum() == 0) {
            estado = EstadoRegistro.ANOMALIA;
            observaciones.add("monto en cero");
        }

        // --- Anomalia de tipo: se conserva, reclasificado ---
        String tipoOriginal = fila.getTipo();
        String tipo = TextoNormalizador.normalizar(tipoOriginal);
        if (TextoNormalizador.vacio(tipo) || !TIPOS_VALIDOS.contains(tipo)) {
            estado = EstadoRegistro.ANOMALIA;
            observaciones.add("tipo fuera del catalogo (" + tipoOriginal + ")");
            tipo = TIPO_NO_CLASIFICADO;
        }

        Transaccion transaccion = new Transaccion();
        transaccion.setId(id);
        transaccion.setFecha(fecha);
        transaccion.setFechaOriginal(fila.getFecha());
        transaccion.setMonto(monto);
        transaccion.setTipo(tipo);
        transaccion.setTipoOriginal(tipoOriginal);
        transaccion.setEstado(estado);
        transaccion.setObservaciones(observaciones.length() == 0 ? null : observaciones.toString());
        return transaccion;
    }

    private Long leerId(TransaccionCsv fila) {
        Integer id = NumeroParser.aEntero(fila.getId());
        if (id == null) {
            throw new RegistroInvalidoException("id de transaccion ausente o no numerico", fila.toString());
        }
        return id.longValue();
    }

    private LocalDate leerFecha(TransaccionCsv fila) {
        LocalDate fecha = FechaParser.parsear(fila.getFecha());
        if (fecha == null) {
            throw new RegistroInvalidoException(
                    "fecha invalida para un reporte diario (" + fila.getFecha() + ")", fila.toString());
        }
        return fecha;
    }

    private BigDecimal leerMonto(TransaccionCsv fila) {
        BigDecimal monto = NumeroParser.aImporte(fila.getMonto());
        if (monto == null) {
            throw new RegistroInvalidoException("monto ausente o no numerico", fila.toString());
        }
        return monto;
    }
}
