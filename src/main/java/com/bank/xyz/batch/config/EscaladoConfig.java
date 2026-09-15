package com.bank.xyz.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
public class EscaladoConfig {

    private static final Logger log = LoggerFactory.getLogger(EscaladoConfig.class);

    @Bean(name = "ejecutorParticiones", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor ejecutorParticiones(BatchProperties propiedades) {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(propiedades.getHilosCore());
        ejecutor.setMaxPoolSize(propiedades.getHilosMax());
        ejecutor.setQueueCapacity(propiedades.getCola());
        ejecutor.setThreadNamePrefix("particion-");
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(60);
        ejecutor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        ejecutor.initialize();

        log.info("Pool de particiones: core={} max={} cola={} | particiones configuradas={}",
                propiedades.getHilosCore(), propiedades.getHilosMax(),
                propiedades.getCola(), propiedades.getParticiones());
        return ejecutor;
    }
}
