package com.bank.xyz.pagos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Banco XYZ - Procesamiento de Pagos",
        version = "1.0",
        description = "Depositos, transferencias y pagos de servicios. Publica cada operacion confirmada y las alertas de seguridad."))
public class PagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagosApplication.class, args);
    }
}
