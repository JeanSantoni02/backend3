package com.bank.xyz.bff.atm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CoreApiConfig {

    @Bean
    public RestClient coreApi(@Value("${banco.core-api.url}") String url,
                              @Value("${banco.core-api.timeout-conexion-ms:2000}") int timeoutConexion,
                              @Value("${banco.core-api.timeout-lectura-ms:8000}") int timeoutLectura) {

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutConexion));
        // Timeout de lectura mas largo que en los otros BFF: un retiro toma un
        // bloqueo sobre la cuenta y cortar la espera antes de tiempo dejaria al
        // cajero sin saber si el debito se aplico o no.
        fabrica.setReadTimeout(Duration.ofMillis(timeoutLectura));

        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }
}
