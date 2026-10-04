package com.bank.xyz.notificaciones.repositorio;

import com.bank.xyz.notificaciones.modelo.Notificacion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class RepositorioNotificaciones {

    private static final int MAXIMO = 200;

    private final ConcurrentLinkedDeque<Notificacion> notificaciones = new ConcurrentLinkedDeque<>();

    // Ids ya procesados: un mismo evento puede reentregarse y no debe
    // generar dos notificaciones
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    public boolean yaProcesado(String eventoId) {
        return !procesados.add(eventoId);
    }

    public void guardar(Notificacion n) {
        notificaciones.addFirst(n);
        while (notificaciones.size() > MAXIMO) {
            notificaciones.removeLast();
        }
    }

    public List<Notificacion> ultimas(int cantidad) {
        return notificaciones.stream().limit(cantidad).toList();
    }

    public List<Notificacion> porCuenta(Integer cuentaId) {
        return notificaciones.stream().filter(n -> n.cuentaId().equals(cuentaId)).toList();
    }

    public int total() {
        return notificaciones.size();
    }

    public Collection<Notificacion> todas() {
        return new ArrayList<>(notificaciones);
    }
}
