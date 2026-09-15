package com.bank.xyz.bff.mobile;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - BFF Movil",
        version = "1.0",
        description = "Backend dedicado a la app movil. Devuelve solo los datos "
                + "esenciales para reducir consumo de datos y latencia."))
public class BffMobileApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffMobileApplication.class, args);
    }
}
