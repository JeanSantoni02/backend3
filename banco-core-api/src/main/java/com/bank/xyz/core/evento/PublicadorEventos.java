package com.bank.xyz.core.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final JmsTemplate jmsTemplate;
    private final String destino;

    public PublicadorEventos(JmsTemplate jmsTemplate,
                             @Value("${banco.eventos.destino:banco.eventos.transaccion}") String destino) {
        this.jmsTemplate = jmsTemplate;
        this.destino = destino;
    }

    // Publicar nunca debe tumbar la operacion que lo origino: el retiro ya
    // quedo confirmado en la base cuando llega aqui
    public void publicar(EventoTransaccion evento) {
        try {
            jmsTemplate.convertAndSend(destino, evento);
            log.info("Evento publicado | id={} tipo={} cuenta={} destino={}",
                    evento.getEventoId(), evento.getTipo(), evento.getCuentaId(), destino);
        } catch (Exception e) {
            log.error("No se pudo publicar el evento {}: {}", evento.getEventoId(), e.getMessage());
        }
    }
}
