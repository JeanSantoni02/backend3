package com.bank.xyz.core.config;

import com.bank.xyz.core.evento.EventoTransaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
public class JmsConfig {

    // El evento viaja como JSON; el consumidor no necesita la clase del productor.
    // Se reutiliza el ObjectMapper de Spring porque el que crea el conversor por
    // defecto no sabe serializar LocalDateTime.
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
