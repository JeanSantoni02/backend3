package com.bank.xyz.bff.atm;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - BFF Cajeros",
        version = "1.0",
        description = "Backend dedicado a los cajeros automaticos. Superficie minima, "
                + "autenticacion por terminal, validaciones de denominacion y auditoria."))
public class BffAtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffAtmApplication.class, args);
    }
}
