package com.bank.xyz.batch.processor;

import com.bank.xyz.batch.model.Cuenta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class InteresProcessor implements ItemProcessor<Cuenta, Cuenta> {

    // 🔽 AGREGAR ESTA LÍNEA 🔽
    private static final Logger log = LoggerFactory.getLogger(InteresProcessor.class);

    @Override
    public Cuenta process(Cuenta cuenta) throws Exception {
        StringBuilder errores = new StringBuilder();

        // Validar saldo
        if (cuenta.getSaldo() == null) {
            errores.append("Saldo nulo; ");
            cuenta.setSaldo(0.0);
        }

        // Validar edad
        if (cuenta.getEdad() == null || cuenta.getEdad() < 0 || cuenta.getEdad() > 150) {
            errores.append("Edad inválida: ").append(cuenta.getEdad()).append("; ");
            cuenta.setEdad(0);
        }

        // Validar tipo
        String tipo = cuenta.getTipo();
        if (tipo == null || tipo.isEmpty() || tipo.equals("-1") || 
            tipo.equalsIgnoreCase("unknown") || tipo.equalsIgnoreCase("desconocido")) {
            errores.append("Tipo inválido: ").append(tipo).append("; ");
            cuenta.setTipo("unknown");
        }

        if (!errores.isEmpty()) {
            log.warn("Cuenta ID {} tiene errores: {}", cuenta.getCuentaId(), errores);
        }

        return cuenta;
    }
}