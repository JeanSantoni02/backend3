package com.bank.xyz.cuentas.config;

import com.bank.xyz.cuentas.evento.EventoTransaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
public class JmsConfig {

    // Mismo contrato JSON que usa el resto de los productores
    @Bean
    public MessageConverter convertidorJson(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter conv = new MappingJackson2MessageConverter();
        conv.setObjectMapper(objectMapper);
        conv.setTargetType(MessageType.TEXT);
        conv.setTypeIdPropertyName("_tipo");
        conv.setTypeIdMappings(Map.of("eventoTransaccion", EventoTransaccion.class));
        return conv;
    }
}
