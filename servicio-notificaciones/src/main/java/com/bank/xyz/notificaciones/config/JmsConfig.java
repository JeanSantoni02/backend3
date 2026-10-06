package com.bank.xyz.notificaciones.config;

import com.bank.xyz.notificaciones.modelo.AlertaSeguridad;
import com.bank.xyz.notificaciones.modelo.EventoTransaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
public class JmsConfig {

    // JSON para que productor y consumidor no dependan de la misma clase Java
    @Bean
    public MessageConverter convertidorJson(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter conv = new MappingJackson2MessageConverter();
        conv.setObjectMapper(objectMapper);
        conv.setTargetType(MessageType.TEXT);
        conv.setTypeIdPropertyName("_tipo");
        conv.setTypeIdMappings(java.util.Map.of(
                "eventoTransaccion", EventoTransaccion.class,
                "alertaSeguridad", AlertaSeguridad.class));
        return conv;
    }

    // pubSubDomain: es un topico, cada suscriptor recibe todos los mensajes
    @Bean
    public DefaultJmsListenerContainerFactory fabricaTopicos(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer,
            MessageConverter convertidorJson) {

        DefaultJmsListenerContainerFactory fabrica = new DefaultJmsListenerContainerFactory();
        configurer.configure(fabrica, connectionFactory);
        fabrica.setPubSubDomain(true);
        fabrica.setMessageConverter(convertidorJson);
        return fabrica;
    }
}
