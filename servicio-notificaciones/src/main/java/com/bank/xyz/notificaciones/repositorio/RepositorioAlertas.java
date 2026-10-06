package com.bank.xyz.notificaciones.repositorio;

import com.bank.xyz.notificaciones.modelo.AlertaSeguridad;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class RepositorioAlertas {

    private static final int MAXIMO = 200;

    private final ConcurrentLinkedDeque<AlertaSeguridad> alertas = new ConcurrentLinkedDeque<>();
    private final Set<String> registradas = ConcurrentHashMap.newKeySet();

    // false si la alerta ya habia llegado antes
    public boolean registrar(AlertaSeguridad alerta) {
        if (!registradas.add(alerta.getAlertaId())) {
            return false;
        }
        alertas.addFirst(alerta);
        while (alertas.size() > MAXIMO) {
            alertas.removeLast();
        }
        return true;
    }

    public List<AlertaSeguridad> ultimas(int cantidad) {
        return alertas.stream().limit(cantidad).toList();
    }

    public int total() {
        return alertas.size();
    }
}
