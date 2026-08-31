package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.model.Transaccion;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@Component
public class TransaccionProcessor implements ItemProcessor<Transaccion, Transaccion> {

    private static final List<String> TIPOS_VALIDOS = Arrays.asList("debito", "credito", "deposito", "retiro", "compra", "pago");

    @Override
    public Transaccion process(Transaccion transaccion) throws Exception {
        StringBuilder errores = new StringBuilder();

        // Validar fecha
        String fechaStr = transaccion.getFecha() != null ? transaccion.getFecha().toString() : null;
        if (fechaStr != null && !fechaStr.isEmpty()) {
            try {
                LocalDate fecha = parseFecha(fechaStr);
                transaccion.setFecha(fecha);
            } catch (DateTimeParseException e) {
                errores.append("Fecha inválida: ").append(fechaStr).append("; ");
                transaccion.setFecha(null);
            }
        }

        // Validar monto
        if (transaccion.getMonto() == null) {
            errores.append("Monto nulo; ");
        } else if (transaccion.getMonto() < 0) {
            transaccion.setEsAnomalia(true);
            errores.append("Monto negativo: ").append(transaccion.getMonto()).append("; ");
        }

        // Validar tipo
        String tipo = transaccion.getTipo();
        if (tipo == null || tipo.isEmpty() || !TIPOS_VALIDOS.contains(tipo.toLowerCase())) {
            errores.append("Tipo inválido: ").append(tipo).append("; ");
            transaccion.setTipo("DESCONOCIDO");
        }

        if (!errores.isEmpty()) {
            transaccion.setComentarioError(errores.toString());
            transaccion.setEsAnomalia(true);
        }

        return transaccion;
    }

    private LocalDate parseFecha(String fechaStr) {
        List<String> formatos = Arrays.asList(
                "yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy",
                "dd/MM/yyyy", "MM-dd-yyyy", "MM/dd/yyyy"
        );
        for (String formato : formatos) {
            try {
                return LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern(formato));
            } catch (DateTimeParseException e) {}
        }
        throw new DateTimeParseException("No se pudo parsear: " + fechaStr, fechaStr, 0);
    }
}