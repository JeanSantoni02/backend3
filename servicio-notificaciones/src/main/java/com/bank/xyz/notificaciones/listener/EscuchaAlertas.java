package com.bank.xyz.notificaciones.listener;

import com.bank.xyz.notificaciones.modelo.AlertaSeguridad;
import com.bank.xyz.notificaciones.repositorio.RepositorioAlertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

// Las alertas van por su propio topico para escucharlas sin recibir todas las transacciones
@Component
public class EscuchaAlertas {

    private static final Logger log = LoggerFactory.getLogger(EscuchaAlertas.class);

    private final RepositorioAlertas repositorio;

    public EscuchaAlertas(RepositorioAlertas repositorio) {
        this.repositorio = repositorio;
    }

    @JmsListener(destination = "${banco.eventos.alertas:banco.eventos.alertas}",
            containerFactory = "fabricaTopicos")
    public void recibir(AlertaSeguridad alerta) {
        if (alerta == null || alerta.getAlertaId() == null) {
            return;
        }
        if (!repositorio.registrar(alerta)) {
            log.info("Alerta {} ya registrada, se ignora la reentrega", alerta.getAlertaId());
            return;
        }
        log.warn("ALERTA {} | tipo={} cuenta={} monto={} | {}",
                alerta.getSeveridad(), alerta.getTipo(), alerta.getCuentaId(),
                alerta.getMonto(), alerta.getDetalle());
    }
}
