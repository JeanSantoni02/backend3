package com.bank.xyz.cuentas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - Gestion de Cuentas",
        version = "1.0",
        description = "Apertura, cierre y mantenimiento de cuentas. Valida al titular contra el servicio de clientes."))
public class CuentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuentasApplication.class, args);
    }
}
