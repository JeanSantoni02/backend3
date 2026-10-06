package com.bank.xyz.pagos.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final JmsTemplate jms;
    private final String topicoTransacciones;
    private final String topicoAlertas;

    public PublicadorEventos(JmsTemplate jms,
                             @Value("${banco.eventos.destino:banco.eventos.transaccion}") String topicoTransacciones,
                             @Value("${banco.eventos.alertas:banco.eventos.alertas}") String topicoAlertas) {
        this.jms = jms;
        this.topicoTransacciones = topicoTransacciones;
        this.topicoAlertas = topicoAlertas;
    }

    // Una transaccion completada se avisa solo si el commit se concreta
    public void transaccionAlConfirmar(EventoTransaccion evento) {
        alConfirmar(() -> enviar(topicoTransacciones, evento, evento.getEventoId()));
    }

    public void alertaAlConfirmar(AlertaSeguridad alerta) {
        alConfirmar(() -> enviar(topicoAlertas, alerta, alerta.getAlertaId()));
    }

    // Para intentos rechazados: no hay commit que esperar, el intento mismo es la senal
    public void alertaInmediata(AlertaSeguridad alerta) {
        enviar(topicoAlertas, alerta, alerta.getAlertaId());
    }

    private void alConfirmar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }

    private void enviar(String topico, Object mensaje, String id) {
        try {
            jms.convertAndSend(topico, mensaje);
            log.info("Publicado en {} | id={}", topico, id);
        } catch (Exception e) {
            log.error("No se pudo publicar {} en {}: {}", id, topico, e.getMessage());
        }
    }
}
