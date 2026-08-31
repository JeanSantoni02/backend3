package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.model.CuentaAnual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@Component
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnual, CuentaAnual> {

    // 🔽 AGREGAR ESTA LÍNEA 🔽
    private static final Logger log = LoggerFactory.getLogger(CuentaAnualProcessor.class);

    @Override
    public CuentaAnual process(CuentaAnual cuentaAnual) throws Exception {
        StringBuilder errores = new StringBuilder();

        // Validar fecha
        String fechaStr = cuentaAnual.getFecha().toString();
        if (fechaStr != null && !fechaStr.isEmpty()) {
            try {
                LocalDate fecha = parseFecha(fechaStr);
                cuentaAnual.setFecha(fecha);
            } catch (DateTimeParseException e) {
                errores.append("Fecha inválida: ").append(fechaStr).append("; ");
                cuentaAnual.setFecha(null);
            }
        } else {
            errores.append("Fecha nula o vacía; ");
        }

        // Validar monto
        if (cuentaAnual.getMonto() == null) {
            errores.append("Monto nulo; ");
            cuentaAnual.setMonto(0.0);
        }

        // Validar descripción
        if (cuentaAnual.getDescripcion() == null || cuentaAnual.getDescripcion().isEmpty()) {
            errores.append("Descripción vacía; ");
            cuentaAnual.setDescripcion("SIN DESCRIPCIÓN");
        }

        if (!errores.isEmpty()) {
            log.warn("Cuenta Anual ID {} tiene errores: {}", cuentaAnual.getCuentaId(), errores);
        }

        return cuentaAnual;
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