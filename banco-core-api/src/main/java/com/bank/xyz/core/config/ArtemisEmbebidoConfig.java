package com.bank.xyz.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.artemis.embedded.enabled", havingValue = "true",
        matchIfMissing = false)
public class ArtemisEmbebidoConfig {

    private static final Logger log = LoggerFactory.getLogger(ArtemisEmbebidoConfig.class);

    // El broker embebido solo acepta conexiones dentro de la misma JVM. Con
    // este acceptor queda escuchando en el puerto 61616 y el servicio de
    // notificaciones puede suscribirse desde otro proceso sin levantar Docker.
    @Bean
    public ArtemisConfigurationCustomizer acceptorTcp() {
        return configuracion -> {
            try {
                configuracion.addAcceptorConfiguration("netty", "tcp://0.0.0.0:61616");
                configuracion.setSecurityEnabled(false);
                configuracion.setPersistenceEnabled(false);
                log.info("Broker Artemis embebido escuchando en tcp://0.0.0.0:61616");
            } catch (Exception e) {
                log.warn("No se pudo abrir el acceptor TCP del broker embebido: {}",
                        e.getMessage());
            }
        };
    }
}
