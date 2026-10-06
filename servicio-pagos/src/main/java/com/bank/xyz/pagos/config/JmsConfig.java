package com.bank.xyz.pagos.config;

import com.bank.xyz.pagos.evento.AlertaSeguridad;
import com.bank.xyz.pagos.evento.EventoTransaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
public class JmsConfig {

    @Bean
    public MessageConverter convertidorJson(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter conv = new MappingJackson2MessageConverter();
        conv.setObjectMapper(objectMapper);
        conv.setTargetType(MessageType.TEXT);
        conv.setTypeIdPropertyName("_tipo");
        conv.setTypeIdMappings(Map.of(
                "eventoTransaccion", EventoTransaccion.class,
                "alertaSeguridad", AlertaSeguridad.class));
        return conv;
    }
}
