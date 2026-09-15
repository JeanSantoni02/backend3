package com.bank.xyz.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - Servicio de dominio",
        version = "1.0",
        description = "API generica sobre los datos cargados por el batch. "
                + "La consumen los tres BFF; ningun cliente la usa directamente."))
public class BancoCoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BancoCoreApiApplication.class, args);
    }
}
