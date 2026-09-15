package com.bank.xyz.bff.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class EjecutorConfig {

    // Pool acotado para el fan-out hacia el servicio de dominio. Son cuatro
    // llamadas por peticion y lo que hacen es esperar red, no calcular, asi que
    // conviene un pool que crezca con la demanda y devuelva los hilos al quedar
    // ociosos. El tope evita que una avalancha de peticiones al portal termine
    // saturando al servicio de dominio.
    @Bean(destroyMethod = "shutdown")
    public ExecutorService ejecutorBff() {
        ThreadPoolExecutor ejecutor = new ThreadPoolExecutor(
                8, 64, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        ejecutor.setThreadFactory(Executors.defaultThreadFactory());
        return ejecutor;
    }
}
