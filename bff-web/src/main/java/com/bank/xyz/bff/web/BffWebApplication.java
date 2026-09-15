package com.bank.xyz.bff.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - BFF Web",
        version = "1.0",
        description = "Backend dedicado al portal de navegador. Agrega en una sola "
                + "respuesta los datos que la pantalla necesita."))
public class BffWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffWebApplication.class, args);
    }
}
