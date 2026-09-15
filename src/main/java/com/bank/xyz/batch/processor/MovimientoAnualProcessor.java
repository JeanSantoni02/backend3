package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.dto.MovimientoAnualCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.model.EstadoRegistro;
import com.bank.xyz.batch.model.MovimientoAnual;
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
public class MovimientoAnualProcessor implements ItemProcessor<MovimientoAnualCsv, MovimientoAnual> {

    private static final List<String> TIPOS_VALIDOS = List.of("deposito", "retiro", "compra", "pago");
    private static final String TIPO_NO_CLASIFICADO = "no_clasificado";
    private static final String SIN_DESCRIPCION = "SIN DESCRIPCION";

    @Override
    public MovimientoAnual process(MovimientoAnualCsv fila) {
        StringJoiner observaciones = new StringJoiner("; ");
        EstadoRegistro estado = EstadoRegistro.VALIDO;

        Integer cuentaId = NumeroParser.aEntero(fila.getCuentaId());
        if (cuentaId == null) {
            throw new RegistroInvalidoException("cuenta_id ausente o no numerico", fila.toString());
        }

        LocalDate fecha = FechaParser.parsear(fila.getFecha());
        if (fecha == null) {
            throw new RegistroInvalidoException(
                    "fecha invalida (" + fila.getFecha() + "): el movimiento no se puede imputar a un ejercicio",
                    fila.toString());
        }

        BigDecimal monto = NumeroParser.aImporte(fila.getMonto());
        if (monto == null) {
            throw new RegistroInvalidoException("monto ausente o no numerico", fila.toString());
        }

        if (monto.signum() < 0) {
            estado = EstadoRegistro.CORREGIDO;
            observaciones.add("monto negativo normalizado (origen: " + monto.toPlainString() + ")");
            monto = monto.abs();
        } else if (monto.signum() == 0) {
            estado = EstadoRegistro.ANOMALIA;
            observaciones.add("monto en cero");
        }

        String tipoOriginal = fila.getTransaccion();
        String tipo = TextoNormalizador.normalizar(tipoOriginal);
        if (TextoNormalizador.vacio(tipo) || !TIPOS_VALIDOS.contains(tipo)) {
            estado = EstadoRegistro.ANOMALIA;
            observaciones.add("tipo de movimiento fuera del catalogo (" + tipoOriginal + ")");
            tipo = TIPO_NO_CLASIFICADO;
        }

        String descripcion = fila.getDescripcion();
        if (TextoNormalizador.vacio(descripcion)) {
            if (estado == EstadoRegistro.VALIDO) {
                estado = EstadoRegistro.CORREGIDO;
            }
            observaciones.add("descripcion ausente");
            descripcion = SIN_DESCRIPCION;
        }

        MovimientoAnual movimiento = new MovimientoAnual();
        movimiento.setCuentaId(cuentaId);
        movimiento.setAnio(fecha.getYear());
        movimiento.setFecha(fecha);
        movimiento.setFechaOriginal(fila.getFecha());
        movimiento.setTipoMovimiento(tipo);
        movimiento.setTipoOriginal(tipoOriginal);
        movimiento.setMonto(monto);
        movimiento.setDescripcion(descripcion);
        movimiento.setEstado(estado);
        movimiento.setObservaciones(observaciones.length() == 0 ? null : observaciones.toString());
        return movimiento;
    }
}
