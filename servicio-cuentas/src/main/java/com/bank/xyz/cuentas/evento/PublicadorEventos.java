package com.bank.xyz.cuentas.evento;

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
    private final String destino;

    public PublicadorEventos(JmsTemplate jms,
                             @Value("${banco.eventos.destino:banco.eventos.transaccion}") String destino) {
        this.jms = jms;
        this.destino = destino;
    }

    // Se envia recien cuando la transaccion confirma: si se revierte, no hay aviso
    public void alConfirmar(EventoTransaccion evento) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enviar(evento);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enviar(evento);
            }
        });
    }

    private void enviar(EventoTransaccion evento) {
        try {
            jms.convertAndSend(destino, evento);
            log.info("Evento publicado | id={} tipo={} cuenta={}",
                    evento.getEventoId(), evento.getTipo(), evento.getCuentaId());
        } catch (Exception e) {
            log.error("No se pudo publicar el evento {}: {}", evento.getEventoId(), e.getMessage());
        }
    }
}
