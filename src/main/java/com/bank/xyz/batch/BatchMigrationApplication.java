package com.bank.xyz.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class BatchMigrationApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext contexto =
                SpringApplication.run(BatchMigrationApplication.class, args);
        System.exit(SpringApplication.exit(contexto));
    }
}
